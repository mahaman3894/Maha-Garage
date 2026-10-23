package maha.garage.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 영수증 파싱 결과 모델
 */
data class ParsedReceiptData(
    val fuelAmount: Double? = null,    // 주유량 (L)
    val unitPrice: Int? = null,        // 단가 (원/L)
    val totalPrice: Int? = null,       // 총 결제금액 (원)
    val gasStationName: String? = null // 주유소 상호명
)

/**
 * 국내 주유소(GS칼텍스, SK에너지, 현대오일뱅크, S-OIL, 알뜰 등) 영수증 특화 고성능 파서
 * 
 * - 라벨 기반 정규식 (수량, 단가, 결제금액, 승인금액 등)
 * - 단일 행 다중 컬럼 테이블(품명-단가-수량-금액) 매칭
 * - 주유 도메인 범위 기반 전역 후보군 추출 및 수학적 삼각 검증 (금액 ≈ 단가 × 수량)
 * - 2개 항목 획득 시 나머지 1개 항목 자동 산출 보정
 */
object ReceiptParser {

    private val KNOWN_BRANDS = listOf(
        "GS칼텍스", "GS Caltex", "GS주유소",
        "SK에너지", "SK엔크린", "SK주유소", "SK가스", "SK행복충전",
        "현대오일뱅크", "HD현대오일뱅크", "오일뱅크",
        "S-OIL", "S-Oil", "에쓰오일", "에스오일", "SOIL", "에스-오일", "에쓰-오일",
        "알뜰주유소", "알뜰", "자영알뜰", "ex-OIL", "NH-OIL", "농협주유소",
        "E1", "E1충전소"
    )

    fun parse(rawText: String): ParsedReceiptData {
        if (rawText.isBlank()) return ParsedReceiptData()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var fuelAmount: Double? = null
        var unitPrice: Int? = null
        var totalPrice: Int? = null
        var stationName: String? = null

        // 1. 주유소 상호명 추출 (S-OIL, 알뜰/기타 및 일반 주유소/충전소 상호 파싱)
        val prefixRegex = Regex("""^(?:상호(?:명)?|가맹점(?:명)?|매장명|사업장명|가맹점상호)\s*[:=]?\s*""")
        val stationKeywords = listOf("주유소", "충전소", "셀프", "지점", "에너지", "석유", "가스")
        val trailingMetaRegex = Regex("""\s+(?:\d{2,4}-\d{3,4}-\d{4}|\d{4}[-/.노년]\d{1,2}[-/.월]\d{1,2}|사업자.*)$""")

        for (line in lines) {
            val stripped = line.replace(prefixRegex, "").trim()
            val cleanLine = stripped.replace(Regex("""[\[\]<>#*]"""), " ").trim()

            val matchedBrand = KNOWN_BRANDS.firstOrNull { brand ->
                cleanLine.contains(brand, ignoreCase = true)
            }
            if (matchedBrand != null) {
                val candidate = if (stationKeywords.any { cleanLine.contains(it) }) {
                    val segments = cleanLine.split(Regex("""\s{2,}|\t"""))
                    segments.firstOrNull { it.contains(matchedBrand, ignoreCase = true) } ?: cleanLine
                } else {
                    matchedBrand
                }
                stationName = candidate.replace(trailingMetaRegex, "").trim().take(30)
                break
            } else if ((cleanLine.contains("주유소") || cleanLine.contains("충전소")) && stationName == null) {
                val segments = cleanLine.split(Regex("""\s{2,}|\t"""))
                val best = segments.firstOrNull { it.contains("주유소") || it.contains("충전소") } ?: cleanLine
                stationName = best.replace(trailingMetaRegex, "").trim().take(30)
            }
        }

        // 2. 라벨 기반 정규식 매칭
        // 주유량(L): 5.0 ~ 150.0L
        val literExplicitRegex = Regex("""([0-9]{1,3}\.[0-9]{1,4})\s*(?:L|l|ℓ|리터)""", RegexOption.IGNORE_CASE)
        val literLabelRegex = Regex("""(?:수량|주유량|용량|체적|수\s*량|주유리터|주유\s*L)\s*[:=]?\s*([0-9]{1,3}(?:\.[0-9]{1,4})?)\s*(?:L|l|ℓ|리터)?""", RegexOption.IGNORE_CASE)
        val literUnitSuffixRegex = Regex("""\b([0-9]{1,3}(?:\.[0-9]{1,4})?)\s*(?:L|l|ℓ|리터)\b""", RegexOption.IGNORE_CASE)

        // 단가: 1,000 ~ 3,500원
        val unitPriceLabelRegex = Regex("""(?:단가|단\s*가|리터당|ℓ당|@|단가\(원\))\s*[:=]?\s*([0-9]{1,2}[,]?[0-9]{3})""")
        val unitPriceWonRegex = Regex("""\b([1-3][,]?[0-9]{3})\s*원\b""")

        // 총 결제금액: 8,000 ~ 400,000원
        val totalExplicitRegex = Regex("""(?:결제금액|합계금액|승인금액|총금액|결제\s*금액|합계\s*금액|승인\s*금액|총\s*금액|받을금액|합\s*계|합계|결제)\s*[:=]?\s*([0-9]{1,3}[,]?[0-9]{3}(?:[,]?[0-9]{3})?)\s*원?""")
        val totalSecondaryRegex = Regex("""(?:금액|승인|신용승인|청구금액)\s*[:=]?\s*([0-9]{1,3}[,]?[0-9]{3}(?:[,]?[0-9]{3})?)\s*원?""")

        for (line in lines) {
            // 주유량 매칭
            if (fuelAmount == null) {
                literLabelRegex.find(line)?.let {
                    val candidate = it.groupValues[1].toDoubleOrNull()
                    if (candidate != null && candidate in 3.0..180.0) {
                        fuelAmount = candidate
                    }
                } ?: literExplicitRegex.find(line)?.let {
                    val candidate = it.groupValues[1].toDoubleOrNull()
                    if (candidate != null && candidate in 3.0..180.0) {
                        fuelAmount = candidate
                    }
                } ?: literUnitSuffixRegex.find(line)?.let {
                    val candidate = it.groupValues[1].toDoubleOrNull()
                    if (candidate != null && candidate in 3.0..180.0) {
                        fuelAmount = candidate
                    }
                }
            }

            // 단가 매칭
            if (unitPrice == null) {
                unitPriceLabelRegex.find(line)?.let {
                    val parsed = it.groupValues[1].replace(",", "").toIntOrNull()
                    if (parsed != null && parsed in 1000..3500) {
                        unitPrice = parsed
                    }
                } ?: unitPriceWonRegex.find(line)?.let {
                    val parsed = it.groupValues[1].replace(",", "").toIntOrNull()
                    if (parsed != null && parsed in 1000..3500) {
                        unitPrice = parsed
                    }
                }
            }

            // 총 결제금액 매칭
            if (totalPrice == null) {
                totalExplicitRegex.find(line)?.let {
                    val parsed = it.groupValues[1].replace(",", "").toIntOrNull()
                    if (parsed != null && parsed in 8000..500000) {
                        totalPrice = parsed
                    }
                }
            }
        }

        // 보조 결제금액 검색
        if (totalPrice == null) {
            for (line in lines) {
                totalSecondaryRegex.find(line)?.let {
                    val parsed = it.groupValues[1].replace(",", "").toIntOrNull()
                    if (parsed != null && parsed in 8000..500000 && parsed != unitPrice) {
                        totalPrice = parsed
                    }
                }
                if (totalPrice != null) break
            }
        }

        // 3. 단일 행 테이블 다중 컬럼 파싱 (예: "휘발유 1,650 30.30 50,000" 또는 "1,685 42.15 71,020")
        if (fuelAmount == null || unitPrice == null || totalPrice == null) {
            val numberRegex = Regex("""\b([0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)\b""")
            for (line in lines) {
                if (line.contains("사업자") || line.contains("카드") || line.contains("승인번호") ||
                    line.contains("TEL") || line.contains("전화") || line.contains("날짜") || line.contains("일시")) {
                    continue
                }

                val tokens = numberRegex.findAll(line)
                    .map { it.groupValues[1].replace(",", "") }
                    .mapNotNull { it.toDoubleOrNull() }
                    .toList()

                if (tokens.size >= 2) {
                    for (i in tokens.indices) {
                        for (j in tokens.indices) {
                            if (i == j) continue
                            val a = tokens[i] // 주유량 후보 (3 ~ 150)
                            val b = tokens[j] // 단가 후보 (1000 ~ 3500)
                            if (a in 3.0..150.0 && b in 1000.0..3500.0) {
                                // 1) 라인에 세 번째 숫자가 있고 금액과 일치하는지 확인
                                var foundThird = false
                                for (k in tokens.indices) {
                                    if (k == i || k == j) continue
                                    val c = tokens[k]
                                    if (c in 8000.0..500000.0 && abs(a * b - c) <= (c * 0.03 + 10.0)) {
                                        if (fuelAmount == null) fuelAmount = (a * 100.0).roundToInt() / 100.0
                                        if (unitPrice == null) unitPrice = b.roundToInt()
                                        if (totalPrice == null) totalPrice = c.roundToInt()
                                        foundThird = true
                                        break
                                    }
                                }
                                if (foundThird) break

                                // 2) 세 번째 숫자는 없지만 이미 찾은 totalPrice와 a*b가 일치하는 경우
                                if (totalPrice != null && abs(a * b - totalPrice!!) <= (totalPrice!! * 0.03 + 10.0)) {
                                    if (fuelAmount == null) fuelAmount = (a * 100.0).roundToInt() / 100.0
                                    if (unitPrice == null) unitPrice = b.roundToInt()
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. 전역 도메인 범위 기반 후보군 추출 및 수학적 삼각 검증 (유저 제안: 0~70L, 단가 1000~3000원, 금액 10000~200000원)
        if (fuelAmount == null || unitPrice == null || totalPrice == null) {
            val amountCandidates = mutableListOf<Double>()
            val unitCandidates = mutableListOf<Int>()
            val totalCandidates = mutableListOf<Int>()

            val generalNumberRegex = Regex("""\b([0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)\b""")

            for (line in lines) {
                // 비관련 행 필터링 (전화번호, 사업자등록번호, 승인번호 등)
                if (line.contains("사업자") || line.contains("카드") || line.contains("승인번호") ||
                    line.contains("TEL") || line.contains("전화") || line.contains("가맹점") || line.contains("단말기")) {
                    continue
                }

                generalNumberRegex.findAll(line).forEach { match ->
                    val clean = match.groupValues[1].replace(",", "")
                    val num = clean.toDoubleOrNull() ?: return@forEach

                    // 주유량 후보 (5.0 ~ 150.0L) - 특히 소수점을 포함하거나 10의 배수인 정수
                    if (num in 5.0..150.0) {
                        amountCandidates.add(num)
                    }
                    // 단가 후보 (1,100 ~ 3,200원 정수)
                    if (clean.indexOf('.') == -1 && num in 1100.0..3200.0) {
                        unitCandidates.add(num.toInt())
                    }
                    // 총 결제금액 후보 (9,000 ~ 350,000원 정수)
                    if (clean.indexOf('.') == -1 && num in 9000.0..350000.0) {
                        totalCandidates.add(num.toInt())
                    }
                }
            }

            // 삼각 검증 (a * u ≈ t)
            var bestMatch: Triple<Double, Int, Int>? = null
            var minDiff = Double.MAX_VALUE

            for (a in amountCandidates) {
                for (u in unitCandidates) {
                    val expectedTotal = a * u
                    for (t in totalCandidates) {
                        val diff = abs(expectedTotal - t)
                        if (diff <= (t * 0.02 + 5.0) && diff < minDiff) {
                            minDiff = diff
                            bestMatch = Triple(a, u, t)
                        }
                    }
                }
            }

            bestMatch?.let { (a, u, t) ->
                if (fuelAmount == null) fuelAmount = (a * 100.0).roundToInt() / 100.0
                if (unitPrice == null) unitPrice = u
                if (totalPrice == null) totalPrice = t
            }
        }

        // 5. 상호 계산 및 오차 보정 (2개 값이 존재할 때 나머지 1개 값 산출)
        if (totalPrice == null && fuelAmount != null && unitPrice != null) {
            totalPrice = (fuelAmount * unitPrice).roundToInt()
        } else if (fuelAmount == null && totalPrice != null && unitPrice != null && unitPrice > 0) {
            val calculated = (totalPrice.toDouble() / unitPrice * 100.0).roundToInt() / 100.0
            if (calculated in 3.0..200.0) {
                fuelAmount = calculated
            }
        } else if (unitPrice == null && totalPrice != null && fuelAmount != null && fuelAmount > 0.0) {
            val calculated = (totalPrice / fuelAmount).roundToInt()
            if (calculated in 1000..3500) {
                unitPrice = calculated
            }
        }

        return ParsedReceiptData(
            fuelAmount = fuelAmount,
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            gasStationName = stationName
        )
    }
}
