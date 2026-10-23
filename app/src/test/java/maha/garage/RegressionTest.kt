package maha.garage

import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.*
import maha.garage.domain.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RegressionTest {
    private val car = CarEntity(id = "a", carNumber = "A", modelName = "test", currentMileage = 110000)
    private val item = MaintenanceCatalogEntity(itemCode = "OIL", displayName = "Oil", category = "test")
    private val rule = MaintenanceRuleEntity(carId = "a", itemCode = "OIL", firstIntervalKm = 100000, intervalKm = 40000)
    private fun evaluate(r: MaintenanceRuleEntity = rule, c: CarEntity = car, i: MaintenanceCatalogEntity = item) =
        MaintenanceScheduleEngine.evaluateItem(c, r, i, LocalDate.of(2026, 10, 17))
    private fun record(date: String, km: Int, action: ActionType, condition: String? = null): Pair<ServiceVisitEntity, ServiceLineEntity> {
        val v = ServiceVisitEntity(carId = "a", serviceDate = date, odometerKm = km)
        return v to ServiceLineEntity(visitId = v.id, itemCode = "OIL", actionType = action, conditionValue = condition)
    }
    @Test fun firstMonthUsesRegistrationDate() {
        val r = rule.copy(intervalKm = null, firstIntervalKm = null, intervalMonths = 12)
        // 1. 교환 기록/관리 기준이 없으면 최초 등록일을 마지막 교환일로 임의 추정하지 않음
        val noHistory = evaluate(r, car.copy(firstRegistrationDate = "2024-02-29"))
        assertNull(noHistory.nextDueDateString)
        assertEquals(DueStatus.NEEDS_DATA, noHistory.status)

        // 2. 관리 시작 기준 날짜가 윤년(2024-02-29)인 경우 12개월 후(2025-02-28) 계산 검증
        val baselineResult = evaluate(r.copy(baselineDate = "2024-02-29"), car.copy(firstRegistrationDate = "2024-02-29"))
        assertEquals("2025-02-28", baselineResult.nextDueDateString)

        // 3. 실제 교환 날짜 기준 개월 주기 계산 검증
        val afterReplacement = evaluate(r.copy(lastServiceDate = "2026-02-28"), car.copy(firstRegistrationDate = "2024-02-29"))
        assertEquals("2027-02-28", afterReplacement.nextDueDateString)
    }
    @Test fun initialDateProgressIsExactAtDeadline() {
        // 날짜 기준이 명시되어 있으면 주행거리 기준이 없어도 기한 당일 상태와 진행률을 정확하게 계산
        val r = rule.copy(intervalKm = null, firstIntervalKm = null, intervalMonths = 12, baselineDate = "2025-10-17")
        val result = evaluate(r, car)
        assertEquals(DueStatus.DUE_NOW, result.status)
        assertEquals(1f, result.progressRatio, 0.0001f)

        // 실제 교환일 기준도 동일하게 검증
        val r2 = rule.copy(intervalKm = null, firstIntervalKm = null, intervalMonths = 12, lastServiceDate = "2025-10-17")
        val result2 = evaluate(r2, car)
        assertEquals(DueStatus.DUE_NOW, result2.status)
        assertEquals(1f, result2.progressRatio, 0.0001f)
    }
    @Test fun firstAndRepeat() {
        // 1. 교환 기록 없음: nextDueKm는 null이고 상태는 NEEDS_DATA
        val evalNoRecord = evaluate()
        assertNull(evalNoRecord.nextDueKm)
        assertEquals(DueStatus.NEEDS_DATA, evalNoRecord.status)

        // 2. 관리 시작 기준 설정 ('현재부터 관리 시작'): 기준 100,000km + 주기 40,000km = 140,000km
        // car.currentMileage가 110,000km로 기준을 넘었더라도 계산 결과를 없애지 않음
        val evalBaseline = evaluate(rule.copy(baselineMileage = 100000))
        assertEquals(140000, evalBaseline.nextDueKm)
        assertEquals(30000, evalBaseline.remainingKm)
        assertTrue(evalBaseline.isBaseline)

        // 3. 실제 교환 후 재계산: 실제 교환(110,000km)이 관리 기준보다 우선 적용되어 110,000 + 40,000 = 150,000km
        val evalReplaced = evaluate(rule.copy(baselineMileage = 100000, lastServiceMileage = 110000, lastServiceDate = "2026-10-01"))
        assertEquals(150000, evalReplaced.nextDueKm)
        assertEquals(40000, evalReplaced.remainingKm)
        assertFalse(evalReplaced.isBaseline)
    }
    @Test fun inspectionDoesNotReset() {
        val r = rule.copy(lastServiceMileage = 50000, lastServiceDate = "2026-01-01")
        val result = ServiceHistoryReducer.reduce(r, listOf(record("2026-09-01", 90000, ActionType.INSPECT, "7mm")))
        assertEquals(50000, result.lastServiceMileage)
        assertEquals("2026-01-01", result.lastServiceDate)
        assertEquals("7mm", result.lastConditionValue)
    }
    @Test fun allNonReplacementActionsPreserveCycle() {
        for (action in ActionType.entries.filter { it != ActionType.REPLACE }) {
            val r = rule.copy(lastServiceMileage = 50000, lastServiceDate = "2026-01-01")
            assertEquals(50000, ServiceHistoryReducer.reduce(r, listOf(record("2026-09-01", 90000, action))).lastServiceMileage)
        }
    }
    @Test fun outOfOrderHistory() {
        val recent = record("2026-09-01", 90000, ActionType.REPLACE)
        val old = record("2026-01-01", 50000, ActionType.REPLACE)
        assertEquals(90000, ServiceHistoryReducer.reduce(rule, listOf(recent, old)).lastServiceMileage)
    }
    @Test fun olderVisitCannotOverwriteCache() {
        val r = rule.copy(lastServiceDate = "2026-09-01", lastServiceMileage = 90000)
        assertEquals(90000, ServiceHistoryReducer.reduce(r, listOf(record("2026-01-01", 50000, ActionType.REPLACE))).lastServiceMileage)
    }
    @Test fun replacementClearsOldWear() {
        val r = rule.copy(lastConditionValue = "2mm")
        assertNull(ServiceHistoryReducer.reduce(r, listOf(record("2026-09-01", 90000, ActionType.REPLACE))).lastConditionValue)
    }
    @Test fun emptyInspectionKeepsMeasurement() {
        val r = rule.copy(lastConditionValue = "7mm")
        assertEquals("7mm", ServiceHistoryReducer.reduce(r, listOf(record("2026-09-01", 90000, ActionType.INSPECT))).lastConditionValue)
    }
    @Test(expected = IllegalArgumentException::class) fun crossCarHistoryRejected() {
        val record = record("2026-09-01", 90000, ActionType.REPLACE)
        ServiceHistoryReducer.reduce(rule.copy(carId = "b"), listOf(record))
    }
    @Test(expected = IllegalArgumentException::class) fun crossCarEvaluationRejected() { evaluate(c = car.copy(id = "b")) }
    @Test fun missingDateDoesNotInventJanuaryFirst() {
        val result = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, intervalMonths = 12))
        assertNull(result.nextDueDateString)
        assertEquals(DueStatus.NEEDS_DATA, result.status)
    }
    @Test fun replacementWithMissingMileageDoesNotReuseFirst() {
        assertNull(evaluate(rule.copy(lastServiceDate = "2026-09-01")).nextDueKm)
    }
    @Test fun repeatMonthIgnoresFirstMonth() {
        val result = evaluate(rule.copy(firstIntervalMonths = 60, intervalMonths = 12, lastServiceDate = "2025-10-17", lastServiceMileage = 100000))
        assertEquals("2026-10-17", result.nextDueDateString)
        assertEquals(DueStatus.DUE_NOW, result.status)
        assertEquals(1f, result.progressRatio, 0.0001f)
    }
    @Test fun hoursExactlyDue() {
        // 1. 미입력 시 자동으로 0시간으로 간주하지 않음
        val noHours = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, intervalHours = 100.0), car.copy(engineHours = 100.0))
        assertNull(noHours.nextDueHours)
        assertEquals(DueStatus.NEEDS_DATA, noHours.status)

        // 2. 마지막 교환 사용시간 0.0시간 명시 시 정확히 100시간에 도달하면 DUE_NOW
        val result = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, intervalHours = 100.0, lastServiceHours = 0.0), car.copy(engineHours = 100.0))
        assertEquals(100.0, result.nextDueHours)
        assertEquals(DueStatus.DUE_NOW, result.status)
        assertEquals(1f, result.progressRatio, 0.0001f)

        // 3. 관리 시작 시간 50.0시간 명시 후 150시간 도달 시 DUE_NOW
        val baselineResult = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, intervalHours = 100.0, baselineHours = 50.0), car.copy(engineHours = 150.0))
        assertEquals(150.0, baselineResult.nextDueHours)
        assertEquals(DueStatus.DUE_NOW, baselineResult.status)
        assertEquals(1f, baselineResult.progressRatio, 0.0001f)
    }
    @Test fun conditionOnlyWearIsEvaluated() {
        // 교환 날짜·주행거리가 없어도 마모 한계선(<=3mm) 도달 시 OVERDUE 판정
        val overdueEval = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, lastConditionValue = "2.5mm"), i = item.copy(isConditionBased = true))
        assertEquals(DueStatus.OVERDUE, overdueEval.status)
        assertTrue(overdueEval.statusDescription.contains("마모 한계선"))

        // 마모 한계선 미도달(양호) 시 OVERDUE가 아님
        val normalEval = evaluate(rule.copy(intervalKm = null, firstIntervalKm = null, lastConditionValue = "7.0mm"), i = item.copy(isConditionBased = true))
        assertNotEquals(DueStatus.OVERDUE, normalEval.status)
    }
    @Test fun inspectionPeriodUsesDateToo() {
        assertEquals(DueStatus.INSPECTION_DUE, evaluate(rule.copy(intervalMonths = 1, lastServiceDate = "2026-01-01", lastServiceMileage = 100000), i = item.copy(isConditionBased = true)).status)
    }
    @Test fun notificationCooldownAndNewCycle() {
        val now = 1_000_000_000L
        assertFalse(NotificationPolicy.shouldSend(now, now - 1000, "a", "a", 3))
        assertTrue(NotificationPolicy.shouldSend(now, now - 3 * 86_400_000L, "a", "a", 3))
        assertTrue(NotificationPolicy.shouldSend(now, now - 1000, "new-cycle", "old-cycle", 3))
        assertFalse(NotificationPolicy.shouldSend(now, 0, "", null, 3))
    }
    @Test fun dieselHybridsExcludeIgnition() {
        for (type in listOf(PowertrainType.HEV_DIESEL, PowertrainType.PHEV_DIESEL))
            assertFalse(ApplicabilityResolver.isItemApplicable(car.copy(powertrain = type), item.copy(itemCode = "SPARK_PLUG")))
    }
    @Test fun severeFirstPeriodCannotExceedSevereRepeat() {
        val r = ApplicabilityResolver.resolveDefaultRules(car.copy(isSevereCondition = true), listOf(item.copy(defaultIntervalKm = 40000, firstIntervalKm = 100000, severeIntervalKm = 20000))).single()
        assertEquals(20000, r.firstIntervalKm)
        assertEquals(20000, r.intervalKm)
    }
    @Test(expected = IllegalArgumentException::class) fun invalidTireModeRejected() { TireSwapCalculator.performTireSwap(car, "INVALID") }
    @Test(expected = IllegalArgumentException::class) fun backwardTireSwapRejected() { TireSwapCalculator.performTireSwap(car.copy(lastSwapMileage = 100000), "WINTER", 50000) }

    @Test fun retestUrgentEvaluation() {
        val today = LocalDate.of(2026, 10, 18)
        val carWithRetest = car.copy(
            lastInspectionDate = "2026-10-18",
            retestDeadline = "2026-10-20"
        )
        val eval = VehicleComplianceCalculator.evaluateInspection(carWithRetest, today)
        assertEquals(InspectionStatusType.URGENT, eval.status)
        assertEquals(2L, eval.daysToDeadline)
        assertEquals("재검사 (부적합)", eval.inspectionType)
    }

    @Test fun retestOverdueEvaluation() {
        val today = LocalDate.of(2026, 10, 18)
        val carWithExpiredRetest = car.copy(
            lastInspectionDate = "2026-10-01",
            retestDeadline = "2026-10-15"
        )
        val eval = VehicleComplianceCalculator.evaluateInspection(carWithExpiredRetest, today)
        assertEquals(InspectionStatusType.OVERDUE, eval.status)
        assertEquals(-3L, eval.daysToDeadline)
    }

    @Test fun newScheduleOrRetestBypassesCooldown() {
        val now = 1_000_000_000L
        // 동일 일정 및 동일 서명은 3일 쿨다운 적용
        val sameSig = "URGENT:2026-10-20:null"
        assertFalse(NotificationPolicy.shouldSend(now, now - 1000, sameSig, sameSig, 3))

        // 재검사 기한 신설 등으로 새로운 서명/일정이 되면 즉시 알림 발송 허용
        val newRetestSig = "URGENT:2026-10-20:2026-10-20"
        assertTrue(NotificationPolicy.shouldSend(now, now - 1000, newRetestSig, sameSig, 3))
    }

    @Test fun testBackupRestoreWithBaseline() {
        val ruleWithBaseline = MaintenanceRuleEntity(
            id = "rule-baseline-1",
            carId = "car-test",
            itemCode = "ENGINE_OIL",
            intervalKm = 10000,
            intervalMonths = 12,
            baselineDate = "2026-03-01",
            baselineMileage = 50000,
            baselineHours = 120.0
        )

        // 1. 새 포맷 직렬화 및 역직렬화 검증
        val rObj = org.json.JSONObject().apply {
            put("id", ruleWithBaseline.id)
            put("carId", ruleWithBaseline.carId)
            put("itemCode", ruleWithBaseline.itemCode)
            put("actionType", ruleWithBaseline.actionType.name)
            if (ruleWithBaseline.intervalKm != null) put("intervalKm", ruleWithBaseline.intervalKm)
            if (ruleWithBaseline.baselineDate != null) put("baselineDate", ruleWithBaseline.baselineDate)
            if (ruleWithBaseline.baselineMileage != null) put("baselineMileage", ruleWithBaseline.baselineMileage)
            if (ruleWithBaseline.baselineHours != null) put("baselineHours", ruleWithBaseline.baselineHours)
        }

        assertEquals("2026-03-01", rObj.optString("baselineDate"))
        assertEquals(50000, rObj.optInt("baselineMileage"))
        assertEquals(120.0, rObj.optDouble("baselineHours"), 0.001)

        // 2. 구버전 백업 JSON (baseline 필드가 누락된 경우) 복원 호환성 검증
        val oldRObj = org.json.JSONObject().apply {
            put("id", "old-rule")
            put("carId", "old-car")
            put("itemCode", "ENGINE_OIL")
            put("actionType", "REPLACE")
            put("intervalKm", 10000)
            // baselineDate, baselineMileage, baselineHours 누락
        }

        val restoredBaselineDate = if (oldRObj.has("baselineDate")) oldRObj.optString("baselineDate").ifEmpty { null } else null
        val restoredBaselineMileage = if (oldRObj.has("baselineMileage")) oldRObj.getInt("baselineMileage") else null
        val restoredBaselineHours = if (oldRObj.has("baselineHours")) oldRObj.getDouble("baselineHours") else null

        assertNull("구버전에는 baselineDate가 null로 복원되어야 함", restoredBaselineDate)
        assertNull("구버전에는 baselineMileage가 null로 복원되어야 함", restoredBaselineMileage)
        assertNull("구버전에는 baselineHours가 null로 복원되어야 함", restoredBaselineHours)
    }

    @Test fun testServiceHistoryReducerWearResetAndBaselinePrecedence() {
        val baselineRule = MaintenanceRuleEntity(
            id = "pad-rule",
            carId = car.id,
            itemCode = "BRAKE_PAD_FRONT",
            intervalKm = 40000,
            baselineMileage = 30000,
            lastConditionValue = "2.8mm" // 마모 한계선 도달 상태였음
        )

        // 교환 전 실측치 판정은 OVERDUE
        val beforeReplace = evaluate(baselineRule, car.copy(currentMileage = 35000), item.copy(itemCode = "BRAKE_PAD_FRONT", isConditionBased = true))
        assertEquals(DueStatus.OVERDUE, beforeReplace.status)

        // 신품 교환 기록 추가 (conditionValue 없음)
        val visit = ServiceVisitEntity(
            id = "v1",
            carId = car.id,
            serviceDate = "2026-05-01",
            odometerKm = 35000,
            totalCost = 50000L
        )
        val line = ServiceLineEntity(
            id = "l1",
            visitId = "v1",
            itemCode = "BRAKE_PAD_FRONT",
            actionType = ActionType.REPLACE,
            partsCost = 50000L,
            laborCost = 0L,
            conditionValue = null // 새 부품이라 측정값 없음
        )

        val updatedRule = ServiceHistoryReducer.reduce(baselineRule, listOf(visit to line))
        // 이전 측정값 2.8mm가 지워져야 함
        assertNull("교환 후에는 이전 측정값이 제거되어야 함", updatedRule.lastConditionValue)
        assertEquals(35000, updatedRule.lastServiceMileage)
        assertEquals("2026-05-01", updatedRule.lastServiceDate)

        // 실제 교환 후 평가 시, 이전 관리 기준이 아닌 실제 교환 주행거리(35000 + 40000 = 75000km)를 기준으로 계산
        val afterReplace = evaluate(updatedRule, car.copy(currentMileage = 35000), item.copy(itemCode = "BRAKE_PAD_FRONT", isConditionBased = true))
        assertEquals(75000, afterReplace.nextDueKm)
        assertFalse("실제 교환 기록이 있으면 isBaseline은 false여야 함", afterReplace.isBaseline)
        assertEquals(DueStatus.NORMAL, afterReplace.status)
    }

    @Test fun testExplicitZeroIsDistinctFromNull() {
        // 주행거리 0km 명시
        val zeroKmRule = rule.copy(lastServiceMileage = 0, intervalKm = 10000)
        val zeroKmEval = evaluate(zeroKmRule, car.copy(currentMileage = 5000))
        assertEquals(10000, zeroKmEval.nextDueKm)
        assertEquals(5000, zeroKmEval.remainingKm)

        // 미입력(null) 주행거리
        val nullKmRule = rule.copy(lastServiceMileage = null, baselineMileage = null)
        val nullKmEval = evaluate(nullKmRule, car.copy(currentMileage = 5000))
        assertNull("미입력인 경우 다음 교환 거리는 null이어야 함", nullKmEval.nextDueKm)
        assertNull("미입력인 경우 잔여 거리는 null이어야 함", nullKmEval.remainingKm)
    }

    @Test fun testConditionUnknownDoesNotEvaluateToNormal() {
        // 해석할 수 없는 측정값("unknown", "null", "미확인" 등)은 NORMAL로 처리되지 않고 NEEDS_DATA가 되어야 함
        val conditionItem = item.copy(itemCode = "BRAKE_PAD_FRONT", isConditionBased = true)
        val unknownRule = rule.copy(
            itemCode = "BRAKE_PAD_FRONT",
            lastServiceMileage = null,
            lastServiceDate = null,
            baselineMileage = null,
            baselineDate = null,
            lastConditionValue = "unknown"
        )
        val result = evaluate(unknownRule, car, conditionItem)
        assertEquals(DueStatus.NEEDS_DATA, result.status)
        assertEquals(0f, result.progressRatio, 0.0001f)

        // "미확인" 값도 마찬가지로 무효 처리
        val unconfirmedRule = unknownRule.copy(lastConditionValue = "미확인")
        val unconfirmedResult = evaluate(unconfirmedRule, car, conditionItem)
        assertEquals(DueStatus.NEEDS_DATA, unconfirmedResult.status)
    }

    @Test fun testConditionBasedValidMeasurementAndProgressRatioHiddenWhenNoCriteria() {
        val conditionItem = item.copy(itemCode = "BRAKE_PAD_FRONT", isConditionBased = true)
        // 교환 기준(남은 거리/남은 일수)이 없는 상태에서 유효한 마모 실측치만 있는 경우
        val goodConditionRule = rule.copy(
            itemCode = "BRAKE_PAD_FRONT",
            lastServiceMileage = null,
            lastServiceDate = null,
            baselineMileage = null,
            baselineDate = null,
            lastConditionValue = "9.0mm"
        )
        val goodResult = evaluate(goodConditionRule, car, conditionItem)
        assertEquals(DueStatus.NORMAL, goodResult.status)
        assertTrue(goodResult.statusDescription.contains("9.0mm"))
        // 교환 기준이 없으므로 주기 진행률은 0f이어야 함
        assertEquals(0f, goodResult.progressRatio, 0.0001f)

        // 마모 한계선(3.0mm 이하) 실측치는 즉시 OVERDUE 판정
        val wornRule = goodConditionRule.copy(lastConditionValue = "2.5mm")
        val wornResult = evaluate(wornRule, car, conditionItem)
        assertEquals(DueStatus.OVERDUE, wornResult.status)
        assertTrue(wornResult.statusDescription.contains("마모 한계선"))
    }

    @Test fun testIntegerOverflowGuardedInNextDueKm() {
        // 기준 주행거리 + 주기 주행거리가 32비트 Int 최대값(21억)을 넘어서도 오버플로우로 음수가 되거나 거짓 정상으로 평가되지 않음
        val largeMileageRule = rule.copy(
            lastServiceMileage = 2_000_000_000,
            intervalKm = 500_000_000
        )
        val result = evaluate(largeMileageRule, car.copy(currentMileage = 100_000))
        assertNotNull(result.nextDueKm)
        assertTrue("오버플로우로 음수가 되지 않아야 함", (result.nextDueKm ?: 0) >= 0)
        assertFalse("음수가 되어 정상으로 둔갑하지 않아야 함", (result.remainingKm ?: 0) < 0 && result.status == DueStatus.NORMAL)
    }
}

