package maha.garage.util

/**
 * 차량 계기판 OCR 텍스트에서 누적 주행거리(ODO / TOTAL / km)를 추출하는 파서 엔진.
 *
 * 정규식 매칭 규칙:
 * 1. 'ODO', 'km', 'TOTAL' 키워드와 인접한 5~6자리(또는 4~6자리) 정수 숫자 매칭.
 * 2. 소수점이 포함된 트립 주행거리(Trip 123.4 km) 및 속도계(100 km/h) 오인식 원천 차단.
 * 3. 콤마 포맷(123,456) 및 공백 분리 포맷(123 456) 자동 정규화.
 */
object OdometerParser {

    fun parse(rawText: String): Int? {
        if (rawText.isBlank()) return null

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 1. 단일 라인 내 ODO / TOTAL 키워드 인근 숫자 매칭 (최우선)
        // 예: ODO 123456 km, ODO: 123,456, TOTAL 85400 km
        val odoKeywordPattern = Regex("""(?i)(?:ODO(?:METER)?|TOTAL)\s*[:.]?\s*([0-9]{1,3}(?:[,\s][0-9]{3})+|[0-9]{4,6})""")
        for (line in lines) {
            if (line.contains("TRIP", ignoreCase = true)) continue
            odoKeywordPattern.find(line)?.let { match ->
                val numStr = match.groupValues[1].replace(",", "").replace(" ", "")
                numStr.toIntOrNull()?.let { if (isValidMileage(it)) return it }
            }
        }

        // 2. 단일 라인 내 숫자 + km 매칭 (km/h 배제)
        // 예: 123456 km, 123,456km, 85,210 KM
        val kmPattern = Regex("""(?<!/)\b([0-9]{1,3}(?:[,\s][0-9]{3})+|[0-9]{4,6})\s*(?:km|KM|킬로)(?!/h|/H|\s*h|\s*H)""")
        for (line in lines) {
            if (line.contains("TRIP", ignoreCase = true) || line.contains("km/h", ignoreCase = true)) continue
            kmPattern.find(line)?.let { match ->
                val numStr = match.groupValues[1].replace(",", "").replace(" ", "")
                numStr.toIntOrNull()?.let { if (isValidMileage(it)) return it }
            }
        }

        // 3. 인접 라인 탐색 (위/아래 줄에 ODO, TOTAL 키워드가 있는 경우)
        for (i in lines.indices) {
            val line = lines[i]
            val isKeywordLine = line.matches(Regex("""(?i).*(?:ODO(?:METER)?|TOTAL).*""")) && !line.contains("TRIP", ignoreCase = true)

            if (isKeywordLine) {
                // 같은 줄에서 4~6자리 숫자
                extractCandidateNumber(line)?.let { return it }

                // 다음 줄에서 4~6자리 숫자
                if (i + 1 < lines.size) {
                    extractCandidateNumber(lines[i + 1])?.let { return it }
                }
                // 이전 줄에서 4~6자리 숫자
                if (i - 1 >= 0) {
                    extractCandidateNumber(lines[i - 1])?.let { return it }
                }
            }
        }

        // 4. 일반적인 5~6자리 정수 숫자 패턴 (10,000 ~ 999,999)
        val generalPattern = Regex("""\b([1-9][0-9]{4,5})\b""")
        for (line in lines) {
            if (line.contains("TRIP", ignoreCase = true) || line.contains("km/h", ignoreCase = true)) continue
            generalPattern.find(line)?.let { match ->
                match.groupValues[1].toIntOrNull()?.let { if (isValidMileage(it)) return it }
            }
        }

        // 5. 콤마가 포함된 4~6자리 (예: 123,456 또는 84,520)
        val commaPattern = Regex("""\b([1-9][0-9]{0,2},[0-9]{3})\b""")
        for (line in lines) {
            if (line.contains("TRIP", ignoreCase = true) || line.contains("km/h", ignoreCase = true)) continue
            commaPattern.find(line)?.let { match ->
                val num = match.groupValues[1].replace(",", "").toIntOrNull()
                if (num != null && isValidMileage(num)) return num
            }
        }

        return null
    }

    private fun extractCandidateNumber(text: String): Int? {
        if (text.contains("TRIP", ignoreCase = true) || text.contains("km/h", ignoreCase = true)) return null
        // 소수점이 있는 트립거리는 제거
        val cleaned = text.replace(Regex("""\b[0-9]+\.[0-9]+\b"""), "")
        val match = Regex("""\b([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{4,6})\b""").find(cleaned) ?: return null
        val numStr = match.groupValues[1].replace(",", "")
        val num = numStr.toIntOrNull() ?: return null
        return if (isValidMileage(num)) num else null
    }

    private fun isValidMileage(num: Int): Boolean {
        return num in 100..999999
    }
}
