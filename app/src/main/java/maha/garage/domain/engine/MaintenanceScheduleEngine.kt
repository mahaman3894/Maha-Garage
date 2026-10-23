package maha.garage.domain.engine

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.model.ItemEvaluation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object MaintenanceScheduleEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 특정 소모품 규칙에 대해 차량의 현재 주행거리 및 날짜를 기준으로 잔여 수명과 도래 상태를 평가합니다.
     */
    fun evaluateItem(
        car: CarEntity,
        rule: MaintenanceRuleEntity,
        catalogItem: MaintenanceCatalogEntity,
        today: LocalDate = LocalDate.now()
    ): ItemEvaluation {
        require(car.id == rule.carId && rule.itemCode == catalogItem.itemCode)
        val intervalKm = rule.intervalKm?.takeIf { it > 0 }
        val intervalMonths = rule.intervalMonths?.takeIf { it > 0 }
        val intervalHours = rule.intervalHours?.takeIf { it.isFinite() && it > 0 }

        // 1. 교환 기록 vs 관리 시작 기준 판별
        // 실제 교환 이력 (최우선 기준)
        val hasReplacement = rule.lastServiceDate != null || rule.lastServiceMileage != null || rule.lastServiceHours != null
        // 관리 시작 기준 (교환 기록 없을 때 사용자가 설정한 기산점)
        val hasBaseline = !hasReplacement && (rule.baselineDate != null || rule.baselineMileage != null || rule.baselineHours != null)

        val isBaseline = hasBaseline
        val isUnregistered = !hasReplacement && !hasBaseline

        // 계산 기준값 결정: 실제 교환 기록 우선, 없으면 관리 시작 기준 사용
        val baseMileage: Int? = rule.lastServiceMileage ?: (if (isBaseline) rule.baselineMileage else null)
        val baseDateStr: String? = rule.lastServiceDate ?: (if (isBaseline) rule.baselineDate else null)
        val baseHours: Double? = rule.lastServiceHours ?: (if (isBaseline) rule.baselineHours else null)

        // 2. 주행거리 기준 산출 (독립 계산, 오버플로우 방지)
        // 다음 교환 주행거리 = 마지막 교환(또는 관리기준) 주행거리 + 설정한 거리 주기 (0km도 유효)
        val nextDueKm: Int? = if (baseMileage != null && intervalKm != null) {
            val sum = baseMileage.toLong() + intervalKm.toLong()
            if (sum > Int.MAX_VALUE) Int.MAX_VALUE else if (sum < 0L) 0 else sum.toInt()
        } else {
            null
        }
        val remainingKm: Int? = nextDueKm?.let {
            val diff = it.toLong() - car.currentMileage.toLong()
            when {
                diff > Int.MAX_VALUE -> Int.MAX_VALUE
                diff < Int.MIN_VALUE -> Int.MIN_VALUE
                else -> diff.toInt()
            }
        }

        // 3. 날짜 기준 산출 (독립 계산, 주행거리 기준이 없어도 날짜 기준이 있으면 계산)
        val baseDate: LocalDate? = baseDateStr?.let { dateStr ->
            runCatching { LocalDate.parse(dateStr, dateFormatter) }.getOrNull()
        }
        val nextDueDateString: String? = if (baseDate != null && intervalMonths != null) {
            baseDate.plusMonths(intervalMonths.toLong()).format(dateFormatter)
        } else {
            null
        }
        val remainingDays: Long? = nextDueDateString?.let {
            val nextDueDate = LocalDate.parse(it, dateFormatter)
            ChronoUnit.DAYS.between(today, nextDueDate)
        }

        // 4. 엔진 가동시간 기준 산출 (독립 계산, 미입력 시간을 자동으로 0시간으로 간주하지 않음)
        val nextDueHours: Double? = if (baseHours != null && intervalHours != null) {
            (baseHours + intervalHours).takeIf { it.isFinite() }
        } else {
            null
        }
        val remainingHours: Double? = if (nextDueHours != null && car.engineHours != null) {
            nextDueHours - car.engineHours
        } else {
            null
        }

        // 5. 소모 진행도 비율 (0.0: 신품 ~ 1.0: 만기 도달)
        // 유효한 교환 기록 또는 관리 시작 기준이 있고 주기 기준이 존재하는 항목만 주기 진행률을 계산
        val progressRatio: Float = if (isUnregistered || (remainingKm == null && remainingDays == null && remainingHours == null)) {
            0f
        } else {
            calculateProgressRatio(
                car = car,
                baseMileage = baseMileage,
                intervalKm = intervalKm,
                baseDate = baseDate,
                intervalMonths = intervalMonths,
                remainingDays = remainingDays,
                baseHours = baseHours,
                intervalHours = intervalHours,
                remainingHours = remainingHours
            )
        }

        // 6. 도래 상태 (DueStatus) 판정
        val (status, statusDesc) = determineStatus(
            catalogItem = catalogItem,
            rule = rule,
            isUnregistered = isUnregistered,
            isBaseline = isBaseline,
            remainingKm = remainingKm,
            remainingDays = remainingDays,
            remainingHours = remainingHours
        )

        val effectiveDisplayName = when (rule.itemCode) {
            "TIMING_BELT_SET" -> "타이밍벨트(겉벨트)"
            "TIMING_BELT_WATER_PUMP" -> "타이밍벨트(워터펌프)"
            "BATTERY_12V" -> "12V 시동 배터리"
            "ENGINE_AIR_FILTER" -> "엔진 흡기 에어크리너"
            "BRAKE_PAD_FRONT" -> "전륜 브레이크 패드"
            "BRAKE_PAD_REAR" -> "후륜 브레이크 패드"
            else -> catalogItem.displayName
        }

        val effectiveGuideNotes = when (rule.itemCode) {
            "TIMING_BELT_SET" -> "표시 주기: 80,000 ~ 100,000 km (8만km부터 점검 및 옐로우 알림)"
            "TIMING_BELT_WATER_PUMP" -> "표시 주기: 100,000 ~ 130,000 km (10만km부터 점검 및 옐로우 알림)"
            "BATTERY_12V" -> "표시 주기: 약 50,000 ~ 60,000 km / 3년 ~ 4년 (기간 기준 우선)"
            "ENGINE_AIR_FILTER" -> "표시 주기: 15,000 ~ 20,000 km (15,000km부터 점검/교체)"
            "BRAKE_PAD_FRONT" -> "표시 주기: 30,000 ~ 40,000 km (30,000km부터 점검)"
            "BRAKE_PAD_REAR" -> "표시 주기: 50,000 ~ 70,000 km (50,000km부터 점검)"
            else -> catalogItem.guideNotes
        }

        return ItemEvaluation(
            itemCode = rule.itemCode,
            displayName = effectiveDisplayName,
            category = catalogItem.category,
            priority = catalogItem.priority,
            actionType = rule.actionType,
            status = status,
            statusDescription = statusDesc,
            remainingKm = remainingKm,
            remainingDays = remainingDays,
            remainingHours = remainingHours,
            progressRatio = progressRatio,
            nextDueKm = nextDueKm,
            nextDueDateString = nextDueDateString,
            nextDueHours = nextDueHours,
            lastServiceDate = rule.lastServiceDate,
            lastServiceMileage = rule.lastServiceMileage,
            lastServiceHours = rule.lastServiceHours,
            lastConditionValue = rule.lastConditionValue,
            intervalKm = intervalKm,
            intervalMonths = intervalMonths,
            intervalHours = intervalHours,
            sourceType = rule.sourceType,
            isConditionBased = catalogItem.isConditionBased,
            guideNotes = effectiveGuideNotes,
            isBaseline = isBaseline,
            baselineDate = rule.baselineDate,
            baselineMileage = rule.baselineMileage,
            baselineHours = rule.baselineHours
        )
    }

    private fun calculateProgressRatio(
        car: CarEntity,
        baseMileage: Int?,
        intervalKm: Int?,
        baseDate: LocalDate?,
        intervalMonths: Int?,
        remainingDays: Long?,
        baseHours: Double?,
        intervalHours: Double?,
        remainingHours: Double?
    ): Float {
        // 1. 주행거리 기반 소모 진행률
        val kmRatio: Float? = if (baseMileage != null && intervalKm != null && intervalKm > 0) {
            val driven = (car.currentMileage - baseMileage).coerceAtLeast(0)
            (driven.toFloat() / intervalKm.toFloat()).coerceAtLeast(0f)
        } else null

        // 2. 기간 기반 소모 진행률
        val dateRatio: Float? = if (baseDate != null && intervalMonths != null && intervalMonths > 0 && remainingDays != null) {
            val totalDays = ChronoUnit.DAYS.between(baseDate, baseDate.plusMonths(intervalMonths.toLong())).coerceAtLeast(1)
            val passedDays = (totalDays - remainingDays).coerceAtLeast(0)
            (passedDays.toFloat() / totalDays.toFloat()).coerceAtLeast(0f)
        } else null

        // 3. 엔진 가동시간 기반 소모 진행률
        val hoursRatio: Float? = if (baseHours != null && intervalHours != null && intervalHours > 0 && remainingHours != null) {
            val totalHours = intervalHours
            val passedHours = (totalHours - remainingHours).coerceAtLeast(0.0)
            (passedHours / totalHours).toFloat().coerceAtLeast(0f)
        } else null

        return maxOf(kmRatio ?: 0f, dateRatio ?: 0f, hoursRatio ?: 0f)
    }

    private fun determineStatus(
        catalogItem: MaintenanceCatalogEntity,
        rule: MaintenanceRuleEntity,
        isUnregistered: Boolean,
        isBaseline: Boolean,
        remainingKm: Int?,
        remainingDays: Long?,
        remainingHours: Double?
    ): Pair<DueStatus, String> {
        val conditionMm: Double? = if (catalogItem.isConditionBased) {
            parseValidConditionThickness(rule.lastConditionValue)
        } else null

        // 1. 상태 기반(Condition Based) 부품의 마모 한계선 실측치 판정 (교환 기록 유무와 무관하게 동작)
        if (conditionMm != null && conditionMm <= 3.0) {
            return DueStatus.OVERDUE to "잔여 두께 ${conditionMm}mm: 마모 한계선 도달 (즉시 교체 필요)"
        }

        // 2. 기준값이 없어 계산이 불가능한 경우
        if (remainingKm == null && remainingDays == null && remainingHours == null) {
            return when {
                rule.itemCode in listOf("TIMING_BELT_SET", "TIMING_BELT_WATER_PUMP") -> {
                    DueStatus.NEEDS_DATA to "교환 기록 없음 (이력 확인 권장)"
                }
                conditionMm != null -> {
                    if (conditionMm <= 5.0) {
                        DueStatus.DUE_SOON to "잔여 두께 ${conditionMm}mm: 마모 점검/교체 임박"
                    } else {
                        DueStatus.NORMAL to "잔여 두께 ${conditionMm}mm (마모 상태 양호)"
                    }
                }
                else -> {
                    DueStatus.NEEDS_DATA to "교환 기록 없음"
                }
            }
        }

        // 품목별 옐로우(교체/점검 임박) 임계값 산출
        val soonKmThreshold = when (rule.itemCode) {
            "TIMING_BELT_SET" -> 20000       // 10만km 기준, 8만km 도달(잔여 2만km 이하)부터 옐로우
            "TIMING_BELT_WATER_PUMP" -> 30000 // 13만km 기준, 10만km 도달(잔여 3만km 이하)부터 옐로우
            "BATTERY_12V" -> 10000           // 6만km 기준, 5만km 도달(잔여 1만km 이하)부터 옐로우
            "ENGINE_AIR_FILTER" -> 5000      // 2만km 기준, 1.5만km 도달(잔여 5,000km 이하)부터 옐로우
            "BRAKE_PAD_FRONT" -> 10000       // 4만km 기준, 3만km 도달(잔여 1만km 이하)부터 옐로우
            "BRAKE_PAD_REAR" -> 20000        // 7만km 기준, 5만km 도달(잔여 2만km 이하)부터 옐로우
            else -> 1000
        }
        val soonDaysThreshold = when (rule.itemCode) {
            "BATTERY_12V" -> 365L             // 4년(48개월) 만기 중 3년 경과(잔여 365일 이하)부터 옐로우
            else -> 30L
        }
        val soonHoursThreshold = 20.0

        // 상태 기반(Condition Based) 부품 처리 (예: 브레이크 패드)
        if (catalogItem.isConditionBased) {
            if ((remainingKm != null && remainingKm <= 0) || (remainingDays != null && remainingDays <= 0) || (remainingHours != null && remainingHours <= 0)) {
                return DueStatus.INSPECTION_DUE to "정기 점검 시기 도래 (마모도/잔여량 실측 필요)"
            }
            if (conditionMm != null && conditionMm <= 5.0) {
                return DueStatus.DUE_SOON to "잔여 두께 ${conditionMm}mm: 마모 점검/교체 임박"
            }
            if ((remainingKm != null && remainingKm <= soonKmThreshold) || (remainingDays != null && remainingDays <= soonDaysThreshold) || (remainingHours != null && remainingHours <= soonHoursThreshold)) {
                val reason = when {
                    remainingKm != null && remainingKm <= soonKmThreshold -> "잔여 약 ${remainingKm}km"
                    remainingDays != null && remainingDays <= soonDaysThreshold -> "잔여 약 ${remainingDays}일"
                    else -> "점검 시기 임박"
                }
                return DueStatus.DUE_SOON to "점검/교체 임박 ($reason)"
            }
            return DueStatus.NORMAL to "정상 (정기 점검 주기 내)"
        }

        // 수치 교체 기준 항목 판정
        val isKmOverdue = remainingKm != null && remainingKm < 0
        val isKmDueNow = remainingKm != null && remainingKm == 0
        val isDateOverdue = remainingDays != null && remainingDays < 0
        val isDateDueNow = remainingDays != null && remainingDays == 0L
        val isHourOverdue = remainingHours != null && remainingHours < 0.0
        val isHourDueNow = remainingHours != null && remainingHours == 0.0

        val prefix = if (isBaseline) "(관리 기준) " else ""

        if (isKmOverdue || isDateOverdue || isHourOverdue) {
            val reason = when {
                isKmOverdue && isDateOverdue -> "${-remainingKm!!}km 및 ${-remainingDays!!}일 초과"
                isKmOverdue -> "${-remainingKm!!}km 초과"
                isDateOverdue -> "${-remainingDays!!}일 초과"
                else -> "권장 가동시간 초과"
            }
            return DueStatus.OVERDUE to "${prefix}교체 시기 지남 ($reason)"
        }

        if (isKmDueNow || isDateDueNow || isHourDueNow) {
            return DueStatus.DUE_NOW to "${prefix}교체 기준 시점 도달 (정비 요망)"
        }

        val isKmSoon = remainingKm != null && remainingKm <= soonKmThreshold
        val isDateSoon = remainingDays != null && remainingDays <= soonDaysThreshold
        val isHourSoon = remainingHours != null && remainingHours <= soonHoursThreshold

        if (isKmSoon || isDateSoon || isHourSoon) {
            val reason = when {
                isKmSoon && isDateSoon -> "${remainingKm}km 또는 ${remainingDays}일 남음"
                isKmSoon -> "${remainingKm}km 남음"
                isDateSoon -> "${remainingDays}일 남음"
                else -> "${remainingHours?.toInt()}시간 남음"
            }
            return DueStatus.DUE_SOON to "${prefix}교체 임박 ($reason)"
        }

        val normalDesc = buildString {
            append("${prefix}정상 (")
            remainingKm?.let { append("잔여 ${it}km") }
            if (remainingKm != null && remainingDays != null) append(", ")
            remainingDays?.let { append("잔여 ${it}일") }
            append(")")
        }
        return DueStatus.NORMAL to normalDesc
    }

    private fun parseValidConditionThickness(value: String?): Double? {
        if (value.isNullOrBlank()) return null
        val trimmed = value.trim()
        val lower = trimmed.lowercase()
        if (lower in listOf("unknown", "null", "none", "n/a", "미확인", "측정불가", "미등록", "오류", "error")) {
            return null
        }
        val mmMatch = Regex("""^(\d+(?:\.\d+)?)\s*mm$""", RegexOption.IGNORE_CASE).find(trimmed)
            ?: Regex("""^(\d+(?:\.\d+)?)$""").find(trimmed)
            ?: Regex("""(\d+(?:\.\d+)?)\s*mm""", RegexOption.IGNORE_CASE).find(trimmed)
        return mmMatch?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
