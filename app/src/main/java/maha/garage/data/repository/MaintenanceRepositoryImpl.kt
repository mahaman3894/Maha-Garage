package maha.garage.data.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.ActionType
import maha.garage.domain.repository.MaintenanceRepository

class MaintenanceRepositoryImpl(
    private val db: MahaGarageDatabase
) : MaintenanceRepository {

    override fun getRulesForCar(carId: String): Flow<List<MaintenanceRuleEntity>> {
        return db.maintenanceRuleDao().getRulesForCar(carId)
    }

    override fun getCatalog(): Flow<List<MaintenanceCatalogEntity>> {
        return db.catalogDao().getAllCatalog()
    }

    override fun getVisitsForCar(carId: String): Flow<List<ServiceVisitWithLines>> {
        return db.serviceVisitDao().getVisitsForCar(carId)
    }

    override fun getVisitsForPeriod(carId: String, fromDate: String?): Flow<List<ServiceVisitWithLines>> = db.serviceVisitDao().getVisitsForPeriod(carId, fromDate)

    override fun getAllVisits(): Flow<List<ServiceVisitWithLines>> {
        return db.serviceVisitDao().getAllVisits()
    }

    override fun getSwapLogsForCar(carId: String): Flow<List<TireSwapLogEntity>> {
        return db.tireSwapLogDao().getSwapLogsForCar(carId)
    }

    override suspend fun recordServiceVisit(
        visit: ServiceVisitEntity,
        lines: List<ServiceLineEntity>
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            requireNotNull(db.carDao().getCarByIdDirect(visit.carId))
            require(java.time.LocalDate.parse(visit.serviceDate) <= java.time.LocalDate.now())
            require(visit.odometerKm >= 0 && visit.totalCost >= 0)
            require(visit.engineHours == null || (visit.engineHours.isFinite() && visit.engineHours >= 0))
            require(lines.all { it.visitId == visit.id && it.partsCost >= 0 && it.laborCost >= 0 })
            require(lines.isNotEmpty()) { "정비 항목을 선택해 주세요." }
            require(lines.map { it.id }.distinct().size == lines.size)
            val actualTotal = lines.fold(0L) { total, line -> Math.addExact(total, Math.addExact(line.partsCost, line.laborCost)) }
            require(actualTotal == visit.totalCost) { "정비 비용 합계가 일치하지 않습니다." }
            require(!db.serviceVisitDao().visitExists(visit.id)) { "이미 저장된 방문 ID입니다. 새 방문 ID를 사용해 주세요." }
            val selectedCar = requireNotNull(db.carDao().getCarByIdDirect(visit.carId))
            val applicableCodes = db.catalogDao().getAllCatalogDirect().filter {
                maha.garage.domain.engine.ApplicabilityResolver.isItemApplicable(selectedCar, it)
            }.map { it.itemCode }.toSet()
            require(lines.all { it.itemCode in applicableCodes }) { "차량에 적용되지 않는 정비 항목입니다." }
            db.serviceVisitDao().recordFullService(visit, lines)
            val history = db.serviceVisitDao().getVisitsForItemsDirect(visit.carId, lines.map { it.itemCode }.distinct())
                .sortedWith(compareBy({ it.visit.serviceDate }, { it.visit.odometerKm }, { it.visit.createdAt }, { it.visit.id }))
            for (rule in db.maintenanceRuleDao().getAllRulesForCarDirect(visit.carId).filter { it.itemCode in lines.map { l -> l.itemCode }.toSet() }) {
                val records = history.flatMap { h -> h.lines.filter { it.itemCode == rule.itemCode }.map { h.visit to it } }
                val updated = maha.garage.domain.engine.ServiceHistoryReducer.reduce(rule, records)
                db.maintenanceRuleDao().updateRule(updated)
            }
            val car = requireNotNull(db.carDao().getCarByIdDirect(visit.carId))
            db.carDao().updateMileage(car.id, maxOf(car.currentMileage, visit.odometerKm),
                listOfNotNull(car.engineHours, visit.engineHours).maxOrNull())
        }
    }

    override suspend fun updateCustomRule(rule: MaintenanceRuleEntity) = updateCustomRules(listOf(rule))

    override suspend fun updateCustomRules(rules: List<MaintenanceRuleEntity>) = withContext(Dispatchers.IO) {
        db.withTransaction {
            require(rules.isNotEmpty() && rules.map { it.carId to it.itemCode }.distinct().size == rules.size)
            val catalog = db.catalogDao().getAllCatalogDirect().associateBy { it.itemCode }
            // Validate every target before writing either axle.
            val updated = rules.map { input ->
                val car = requireNotNull(db.carDao().getCarByIdDirect(input.carId)) { "차량을 찾을 수 없습니다." }
                val item = requireNotNull(catalog[input.itemCode]) { "지원하지 않는 정비 항목입니다." }
                require(maha.garage.domain.engine.ApplicabilityResolver.isItemApplicable(car, item))
                val existing = requireNotNull(db.maintenanceRuleDao().getRuleByItem(input.carId, input.itemCode)) { "정비 기준을 찾을 수 없습니다." }
                maha.garage.domain.engine.MaintenanceInputPolicy.applySettings(existing, input, car)
            }
            updated.forEach { db.maintenanceRuleDao().updateRule(it) }
        }
    }

    override fun getTiresForCar(carId: String): Flow<List<TireEntity>> {
        return db.tireDao().getTiresForCar(carId)
    }

    override fun getMountedTiresForCar(carId: String): Flow<List<TireEntity>> {
        return db.tireDao().getMountedTiresForCar(carId)
    }

    override fun getTireHistoryForCar(carId: String): Flow<List<TireInstallationHistoryEntity>> {
        return db.tireDao().getHistoryForCar(carId)
    }

    override suspend fun saveTires(tires: List<TireEntity>) = withContext(Dispatchers.IO) {
        db.tireDao().insertTires(tires)
    }

    override suspend fun replaceTires(
        carId: String,
        newTires: List<TireEntity>,
        visit: ServiceVisitEntity,
        lines: List<ServiceLineEntity>,
        histories: List<TireInstallationHistoryEntity>,
        wheelAlignmentMileage: Int?
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val car = requireNotNull(db.carDao().getCarByIdDirect(carId))
            // 1. 정비 방문 기록 저장
            db.serviceVisitDao().recordFullService(visit, lines)

            // 2. 규칙 업데이트
            val history = db.serviceVisitDao().getVisitsForCarDirect(carId)
                .sortedWith(compareBy({ it.visit.serviceDate }, { it.visit.odometerKm }, { it.visit.createdAt }, { it.visit.id }))
            for (rule in db.maintenanceRuleDao().getAllRulesForCarDirect(carId)) {
                val records = history.flatMap { h -> h.lines.filter { it.itemCode == rule.itemCode }.map { h.visit to it } }
                val updated = maha.garage.domain.engine.ServiceHistoryReducer.reduce(rule, records)
                db.maintenanceRuleDao().updateRule(updated)
            }

            // 3. 기존 타이어 중 교체 대상 위치의 타이어는 퇴역(RETIRED) 처리
            val existingTires = db.tireDao().getMountedTiresForCarDirect(carId)
            val newPositions = newTires.map { it.position }.toSet()
            for (oldTire in existingTires) {
                if (oldTire.position in newPositions) {
                    val driven = (visit.odometerKm - oldTire.installedAtMileage).coerceAtLeast(0)
                    db.tireDao().updateTire(
                        oldTire.copy(
                            isMounted = false,
                            position = "RETIRED",
                            assignedWheelPosition = oldTire.position,
                            cumulativeMileage = oldTire.cumulativeMileage + driven,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    db.tireDao().insertHistory(
                        TireInstallationHistoryEntity(
                            carId = carId,
                            tireId = oldTire.id,
                            eventType = "REPLACED_RETIRED",
                            fromPosition = oldTire.position,
                            toPosition = "RETIRED",
                            eventDate = visit.serviceDate,
                            odometerKm = visit.odometerKm,
                            drivenDistanceSinceLastEvent = driven,
                            cost = 0L,
                            notes = "신규 타이어 교체로 퇴역: ${oldTire.brand} ${oldTire.productName}".trim()
                        )
                    )
                }
            }

            // 4. 신규 타이어 삽입 (바퀴 위치를 assignedWheelPosition에도 기록)
            db.tireDao().insertTires(newTires.map { it.copy(assignedWheelPosition = it.position) })

            // 5. 이력 저장
            db.tireDao().insertHistories(histories)

            // 6. 차량 정보 갱신 (휠얼라이먼트 정비 주행거리 반영)
            val sampleNewTire = newTires.firstOrNull()
            val newName = sampleNewTire?.let { "${it.brand} ${it.productName} ${it.spec}".trim() }
            val finalWheelAlignmentMileage = wheelAlignmentMileage ?: car.wheelAlignmentMileage
            val updatedCar = if (car.currentTireMode == "WINTER") {
                car.copy(
                    winterTireName = newName ?: car.winterTireName,
                    currentMileage = maxOf(car.currentMileage, visit.odometerKm),
                    wheelAlignmentMileage = finalWheelAlignmentMileage,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                car.copy(
                    summerTireName = newName ?: car.summerTireName,
                    currentMileage = maxOf(car.currentMileage, visit.odometerKm),
                    wheelAlignmentMileage = finalWheelAlignmentMileage,
                    updatedAt = System.currentTimeMillis()
                )
            }
            db.carDao().updateCar(updatedCar)
        }
    }

    override suspend fun rotateTires(
        carId: String,
        currentMileage: Int,
        date: String,
        swapMapping: Map<String, String>,
        cost: Long,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val car = requireNotNull(db.carDao().getCarByIdDirect(carId)) {
                "차량 정보를 찾을 수 없습니다: $carId"
            }
            if (currentMileage < car.currentMileage) {
                throw IllegalArgumentException("위치교환 주행거리(%,d km)는 최신 차량 주행거리(%,d km)보다 작을 수 없습니다.".format(currentMileage, car.currentMileage))
            }

            val mountedTires = db.tireDao().getMountedTiresForCarDirect(carId)
            if (mountedTires.isEmpty()) {
                throw IllegalStateException("현재 장착된 타이어가 없어 위치교환을 수행할 수 없습니다.")
            }

            for (tire in mountedTires) {
                if (currentMileage < tire.installedAtMileage) {
                    throw IllegalArgumentException("위치교환 주행거리(%,d km)는 타이어(${tire.position})의 최근 장착/교환 기준거리(%,d km)보다 작을 수 없습니다.".format(currentMileage, tire.installedAtMileage))
                }
            }

            val standardPositions = setOf("FL", "FR", "RL", "RR")
            val targetPositions = swapMapping.values.toSet()
            if (swapMapping.keys.any { it !in standardPositions } ||
                swapMapping.values.any { it !in standardPositions } ||
                targetPositions.size != swapMapping.size) {
                throw IllegalArgumentException("유효하지 않은 위치 매핑입니다. 각 바퀴(FL, FR, RL, RR)는 1:1로만 이동할 수 있습니다.")
            }

            val updatedTires = mutableListOf<TireEntity>()
            val historyList = mutableListOf<TireInstallationHistoryEntity>()

            for (tire in mountedTires) {
                val oldPos = tire.position
                val newPos = swapMapping[oldPos] ?: oldPos
                val driven = (currentMileage - tire.installedAtMileage).coerceAtLeast(0)
                val newCumulative = tire.cumulativeMileage + driven

                updatedTires.add(
                    tire.copy(
                        position = newPos,
                        assignedWheelPosition = newPos,
                        cumulativeMileage = newCumulative,
                        installedAtMileage = currentMileage,
                        installDate = date,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                if (oldPos != newPos) {
                    historyList.add(
                        TireInstallationHistoryEntity(
                            carId = carId,
                            tireId = tire.id,
                            eventType = "ROTATION",
                            fromPosition = oldPos,
                            toPosition = newPos,
                            eventDate = date,
                            odometerKm = currentMileage,
                            drivenDistanceSinceLastEvent = driven,
                            cost = cost,
                            notes = notes
                        )
                    )
                }
            }

            if (updatedTires.isNotEmpty()) {
                db.tireDao().updateTires(updatedTires)
            }
            if (historyList.isNotEmpty()) {
                db.tireDao().insertHistories(historyList)
            }

            // 차량 주행거리 안전 갱신 (누적거리 절대 감소 불가)
            val newCarMileage = maxOf(car.currentMileage, currentMileage)
            if (newCarMileage != car.currentMileage) {
                db.carDao().updateCar(car.copy(currentMileage = newCarMileage, updatedAt = System.currentTimeMillis()))
                db.meterReadingDao().insertReading(
                    MeterReadingEntity(
                        carId = carId,
                        readingDate = date,
                        odometerKm = newCarMileage,
                        engineHours = car.engineHours,
                        inputSource = "TIRE_ROTATION"
                    )
                )
            }
        }
    }

    override suspend fun swapSeasonTires(
        car: CarEntity,
        targetMode: String,
        swapMileage: Int,
        swapDate: String,
        storageLocation: String?,
        frontTread: Double?,
        rearTread: Double?,
        cost: Long,
        notes: String?
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val freshCar = requireNotNull(db.carDao().getCarByIdDirect(car.id)) {
                "차량 정보를 찾을 수 없습니다: ${car.id}"
            }
            if (swapMileage < freshCar.currentMileage) {
                throw IllegalArgumentException("스왑 시점 주행거리(%,d km)는 최신 차량 주행거리(%,d km)보다 작을 수 없습니다.".format(swapMileage, freshCar.currentMileage))
            }
            if (swapMileage < freshCar.lastSwapMileage) {
                throw IllegalArgumentException("스왑 시점 주행거리(%,d km)는 직전 스왑 주행거리(%,d km)보다 작을 수 없습니다.".format(swapMileage, freshCar.lastSwapMileage))
            }

            val swapResult = maha.garage.domain.engine.TireSwapCalculator.performTireSwap(
                car = freshCar,
                targetMode = targetMode,
                swapMileage = swapMileage,
                swapDate = swapDate,
                storageLocation = storageLocation,
                frontTreadDepthMm = frontTread,
                rearTreadDepthMm = rearTread,
                swapCost = cost,
                notes = notes
            )
            db.tireSwapLogDao().insertSwapLog(swapResult.swapLog)
            db.carDao().updateCar(swapResult.updatedCar)

            // 개별 타이어 엔티티 상태 동기화
            val currentMounted = db.tireDao().getMountedTiresForCarDirect(car.id)
            val allTires = db.tireDao().getTiresForCarDirect(car.id)
            val storedTargetTires = allTires.filter {
                !it.isMounted && it.position == "STORAGE" &&
                (if (targetMode == "WINTER") it.season == "WINTER" else it.season != "WINTER")
            }

            val updatedTires = mutableListOf<TireEntity>()
            val historyList = mutableListOf<TireInstallationHistoryEntity>()

            // 1. 현재 장착 타이어 탈거 보관
            for (tire in currentMounted) {
                val driven = (swapMileage - tire.installedAtMileage).coerceAtLeast(0)
                val originalPos = tire.position
                updatedTires.add(
                    tire.copy(
                        isMounted = false,
                        position = "STORAGE",
                        assignedWheelPosition = originalPos, // 탈거 직전 장착 위치 기억
                        cumulativeMileage = tire.cumulativeMileage + driven,
                        storageLocation = storageLocation,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                historyList.add(
                    TireInstallationHistoryEntity(
                        carId = car.id,
                        tireId = tire.id,
                        eventType = "SEASON_SWAP",
                        fromPosition = originalPos,
                        toPosition = "STORAGE",
                        eventDate = swapDate,
                        odometerKm = swapMileage,
                        drivenDistanceSinceLastEvent = driven,
                        cost = cost,
                        notes = "계절 스왑 탈거: ${storageLocation ?: "보관소"}"
                    )
                )
            }

            // 2. 보관 중이던 타이어 세트 식별 및 FL/FR/RL/RR 1:1 장착 매핑
            val standardPositions = listOf("FL", "FR", "RL", "RR")
            if (storedTargetTires.isEmpty()) {
                // 해당 계절 최초 스왑: 네 위치 타이어 생성하되 기존 계절 주행거리 보존 및 정보 미등록 명시
                val name = if (targetMode == "WINTER") car.winterTireName else car.summerTireName
                val existingSeasonTotal = if (targetMode == "WINTER") car.winterTireMileage else car.summerTireMileage
                val newSeasonTires = standardPositions.map { pos ->
                    TireEntity(
                        carId = car.id,
                        position = pos,
                        assignedWheelPosition = pos,
                        season = targetMode,
                        brand = "미등록",
                        productName = name ?: "정보 미등록",
                        spec = "정보 미등록",
                        cumulativeMileage = existingSeasonTotal,
                        installedAtMileage = swapMileage,
                        installDate = swapDate,
                        isMounted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                }
                db.tireDao().insertTires(newSeasonTires)
                for (t in newSeasonTires) {
                    historyList.add(
                        TireInstallationHistoryEntity(
                            carId = car.id,
                            tireId = t.id,
                            eventType = "SEASON_SWAP",
                            fromPosition = "NEW",
                            toPosition = t.position,
                            eventDate = swapDate,
                            odometerKm = swapMileage,
                            drivenDistanceSinceLastEvent = 0,
                            notes = "계절($targetMode) 최초 등록 장착"
                        )
                    )
                }
            } else {
                val matchedTiresByPos = mutableMapOf<String, TireEntity>()
                val usedTireIds = mutableSetOf<String>()

                // 1차 매핑: assignedWheelPosition(또는 position)이 일치하는 최신 타이어 우선 배정
                for (pos in standardPositions) {
                    val candidatesForPos = storedTargetTires.filter {
                        (it.assignedWheelPosition == pos || it.position == pos) && it.id !in usedTireIds
                    }
                    val best = candidatesForPos.maxByOrNull { it.updatedAt }
                    if (best != null) {
                        matchedTiresByPos[pos] = best
                        usedTireIds.add(best.id)
                    }
                }

                // 2차 매핑: 위치가 지정되지 않은 타이어가 있다면 빈 위치에 보충
                val unassignedPositions = standardPositions.filter { it !in matchedTiresByPos }
                val remainingCandidates = storedTargetTires.filter { it.id !in usedTireIds }
                    .sortedByDescending { it.updatedAt }
                    .toMutableList()

                for (pos in unassignedPositions) {
                    if (remainingCandidates.isNotEmpty()) {
                        val tire = remainingCandidates.removeAt(0)
                        matchedTiresByPos[pos] = tire
                        usedTireIds.add(tire.id)
                    }
                }

                // 검증: 정확히 4개 위치에 각각 1개씩 배정되었는지 검증
                if (matchedTiresByPos.size != 4 || standardPositions.any { it !in matchedTiresByPos }) {
                    throw IllegalStateException("스왑 대상 계절($targetMode)의 보관 타이어 구성이 불완전합니다. (보관 타이어: ${storedTargetTires.size}개, 유효 배정: ${matchedTiresByPos.size}개)")
                }

                for ((pos, tire) in matchedTiresByPos) {
                    updatedTires.add(
                        tire.copy(
                            isMounted = true,
                            position = pos,
                            assignedWheelPosition = pos,
                            installedAtMileage = swapMileage,
                            installDate = swapDate,
                            storageLocation = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    historyList.add(
                        TireInstallationHistoryEntity(
                            carId = car.id,
                            tireId = tire.id,
                            eventType = "SEASON_SWAP",
                            fromPosition = "STORAGE",
                            toPosition = pos,
                            eventDate = swapDate,
                            odometerKm = swapMileage,
                            drivenDistanceSinceLastEvent = 0,
                            notes = "계절 스왑 장착 ($targetMode)"
                        )
                    )
                }
            }

            if (updatedTires.isNotEmpty()) {
                db.tireDao().updateTires(updatedTires)
            }
            if (historyList.isNotEmpty()) {
                db.tireDao().insertHistories(historyList)
            }
        }
    }

    override fun getInspectionHistoryForCar(carId: String): Flow<List<CarInspectionHistoryEntity>> {
        return db.carInspectionDao().getInspectionsForCar(carId)
    }

    override suspend fun recordInspectionResult(
        car: CarEntity,
        history: CarInspectionHistoryEntity
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.carInspectionDao().insertInspection(history)
            db.carDao().updateCar(car)
        }
    }
}
