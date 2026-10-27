package maha.garage.ui.vehicle

import maha.garage.ui.tutorial.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceRuleEntity
import maha.garage.domain.model.VehicleWithStatus
import maha.garage.domain.repository.MaintenanceRepository
import maha.garage.domain.repository.VehicleRepository

data class VehicleListUiState(
    val vehiclesWithStatus: List<VehicleWithStatus> = emptyList(),
    val activeVehicles: List<VehicleWithStatus> = emptyList(),
    val dormantVehicles: List<VehicleWithStatus> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean
        get() = activeVehicles.isEmpty() && dormantVehicles.isEmpty()
}

class VehicleViewModel(
    private val vehicleRepository: VehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val demoMode: Boolean = false
) : ViewModel() {

    private val _listState = MutableStateFlow(VehicleListUiState())
    val listState: StateFlow<VehicleListUiState> = _listState.asStateFlow()

    init {
        loadVehicles()
    }

    private fun loadVehicles() {
        viewModelScope.launch {
            vehicleRepository.getAllVehiclesWithStatus().collect { list ->
                val active = list.filter { !it.car.isArchived }
                val dormant = list.filter { it.car.isArchived }
                _listState.update {
                    it.copy(
                        vehiclesWithStatus = list,
                        activeVehicles = active,
                        dormantVehicles = dormant,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun getVehicleWithStatus(vehicleId: String): Flow<VehicleWithStatus?> {
        return vehicleRepository.getVehicleWithStatus(vehicleId)
    }

    /**
     * 차량번호 기준 자동차 기본 제원 자동 조회 (하이브리드 등록용)
     */
    fun lookupVehicleSpecs(
        plateNumber: String,
        onResult: (maha.garage.data.remote.dto.VehicleSpecDto?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (demoMode) { onResult(TutorialFixtures.spec); return }
        viewModelScope.launch {
            try {
                val spec = vehicleRepository.fetchVehicleSpecByPlate(plateNumber)
                onResult(spec)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "차량 제원 조회 중 오류가 발생했습니다.")
            }
        }
    }

    fun saveVehicle(car: CarEntity, isNewCar: Boolean = false, onComplete: () -> Unit) {
        viewModelScope.launch {
            vehicleRepository.saveVehicle(car, isNewCar = isNewCar)
            ensureInitialTires(car, isNewCar = isNewCar)
            onComplete()
        }
    }

    fun updateMileage(
        carId: String,
        newMileage: Int,
        engineHours: Double? = null,
        summerTireMileage: Int? = null,
        winterTireMileage: Int? = null,
        lastSwapMileage: Int? = null,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                vehicleRepository.updateMileage(
                    carId = carId,
                    newMileage = newMileage,
                    engineHours = engineHours,
                    summerTireMileage = summerTireMileage,
                    winterTireMileage = winterTireMileage,
                    lastSwapMileage = lastSwapMileage
                )
                // Wait for a newly evaluated Room snapshot before allowing navigation.
                vehicleRepository.getVehicleWithStatus(carId).first {
                    it != null && it.car.currentMileage == newMileage
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError("주행거리를 저장하지 못했습니다. 마지막 누적 거리와 입력값을 확인한 뒤 다시 시도하세요.")
                return@launch
            }
            onComplete()
        }
    }

    fun getMountedTires(carId: String): Flow<List<maha.garage.data.local.entity.TireEntity>> {
        return maintenanceRepository.getMountedTiresForCar(carId)
    }

    fun getTireHistory(carId: String): Flow<List<maha.garage.data.local.entity.TireInstallationHistoryEntity>> {
        return maintenanceRepository.getTireHistoryForCar(carId)
    }

    companion object {
        // 앱 실행 중 차량별 타이어 초기화 중복 체크 방지
        private val initializedCarIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    }

    fun ensureInitialTires(car: CarEntity, isNewCar: Boolean = false) {
        if (initializedCarIds.contains(car.id)) return
        viewModelScope.launch {
            if (initializedCarIds.contains(car.id)) return@launch
            // 장착 타이어만이 아니라 보관(STORAGE) 타이어를 포함한 차량의 전체 타이어를 확인
            val allExistingTires = maintenanceRepository.getTiresForCar(car.id).firstOrNull()
            if (!allExistingTires.isNullOrEmpty()) {
                initializedCarIds.add(car.id)
                return@launch
            }

            // 차량의 타이어 정보가 데이터베이스에 아예 없는 최초 1회에만 4개 생성
            val positions = listOf("FL", "FR", "RL", "RR")
            val brand = car.summerTireName?.split(" ")?.firstOrNull() ?: "순정 규격"
            val prod = car.summerTireName?.split(" ")?.drop(1)?.joinToString(" ") ?: "사계절 타이어"
            val setId = "${car.currentTireMode}_SET_1"
            val initialTires = positions.map { pos ->
                maha.garage.data.local.entity.TireEntity(
                    carId = car.id,
                    position = pos,
                    assignedWheelPosition = pos,
                    setId = setId,
                    season = car.currentTireMode,
                    brand = brand,
                    productName = prod,
                    spec = "출고 규격",
                    cumulativeMileage = 0, // 실제 이력이 없는 타이어의 누적거리를 임의 추정하지 않고 0km 신품 기준으로 안전 초기화
                    installedAtMileage = if (isNewCar) 0 else car.currentMileage, // 신차 출고인 경우 0km 장착 기준으로 주행거리 정상 카운트
                    installDate = car.firstRegistrationDate ?: java.time.LocalDate.now().toString(),
                    isMounted = true
                )
            }
            maintenanceRepository.saveTires(initialTires)
            initializedCarIds.add(car.id)
        }
    }

    private val _tireRotationSaving = MutableStateFlow(false)
    val tireRotationSaving = _tireRotationSaving.asStateFlow()
    private val _tireRotationError = MutableStateFlow<String?>(null)
    val tireRotationError = _tireRotationError.asStateFlow()
    fun clearTireRotationError() { _tireRotationError.value = null }

    fun rotateTires(
        carId: String,
        currentMileage: Int,
        date: String,
        swapMapping: Map<String, String>,
        cost: Long,
        notes: String?,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (_tireRotationSaving.value) return
        _tireRotationSaving.value = true
        _tireRotationError.value = null
        viewModelScope.launch {
            try {
                maintenanceRepository.rotateTires(carId, currentMileage, date, swapMapping, cost, notes)
                onSuccess()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                val msg = e.message ?: "위치교환 저장 중 오류가 발생했습니다."
                _tireRotationError.value = msg
                onError(msg)
            } finally {
                _tireRotationSaving.value = false
            }
        }
    }

    fun replaceTires(
        carId: String,
        newTires: List<maha.garage.data.local.entity.TireEntity>,
        visit: maha.garage.data.local.entity.ServiceVisitEntity,
        lines: List<maha.garage.data.local.entity.ServiceLineEntity>,
        histories: List<maha.garage.data.local.entity.TireInstallationHistoryEntity>,
        wheelAlignmentMileage: Int? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            maintenanceRepository.replaceTires(carId, newTires, visit, lines, histories, wheelAlignmentMileage)
            onComplete()
        }
    }

    private val _tireSwapSaving = MutableStateFlow(false)
    val tireSwapSaving = _tireSwapSaving.asStateFlow()
    private val _tireSwapError = MutableStateFlow<String?>(null)
    val tireSwapError = _tireSwapError.asStateFlow()
    fun clearTireSwapError() { _tireSwapError.value = null }

    fun swapTires(
        car: CarEntity,
        targetMode: String,
        swapMileage: Int,
        storageLocation: String?,
        frontTread: Double?,
        rearTread: Double?,
        swapCost: Long,
        notes: String?,
        onComplete: () -> Unit = {}
    ) {
        if (_tireSwapSaving.value) return
        _tireSwapSaving.value = true
        _tireSwapError.value = null
        viewModelScope.launch {
            try {
                maintenanceRepository.swapSeasonTires(
                car = car,
                targetMode = targetMode,
                swapMileage = swapMileage,
                swapDate = java.time.LocalDate.now().toString(),
                storageLocation = storageLocation,
                frontTread = frontTread,
                rearTread = rearTread,
                cost = swapCost,
                notes = notes
            )
                onComplete()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _tireSwapError.value = "스왑 기록을 저장하지 못했습니다. 현재 타이어와 주행거리를 확인하고 다시 시도하세요."
            } finally {
                _tireSwapSaving.value = false
            }
        }
    }

    fun updateRules(rules: List<MaintenanceRuleEntity>, onComplete: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                maintenanceRepository.updateCustomRules(rules)
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "설정을 저장하지 못했습니다.") }
        }
    }

    fun recordServiceVisit(
        visit: maha.garage.data.local.entity.ServiceVisitEntity,
        lines: List<maha.garage.data.local.entity.ServiceLineEntity>,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                maintenanceRepository.recordServiceVisit(visit, lines)
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "정비 기록을 저장하지 못했습니다.") }
        }
    }

    fun archiveVehicle(carId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            vehicleRepository.archiveVehicle(carId)
            onComplete()
        }
    }

    fun unarchiveVehicle(carId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            vehicleRepository.unarchiveVehicle(carId)
            onComplete()
        }
    }

    fun deleteVehiclePermanently(carId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            vehicleRepository.deleteVehiclePermanently(carId)
            onComplete()
        }
    }

    fun updateCarInspectionAndInsurance(
        car: CarEntity,
        inspectionType: String,
        inspectionStartDate: String?,
        inspectionEndDate: String?,
        isInspectionDateEstimated: Boolean,
        insuranceCompany: String?,
        insuranceStartDate: String?,
        insuranceEndDate: String?,
        insuranceEmergencyCall: String?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val updated = car.copy(
                inspectionType = inspectionType,
                inspectionStartDate = inspectionStartDate,
                inspectionEndDate = inspectionEndDate,
                isInspectionDateEstimated = isInspectionDateEstimated,
                insuranceCompany = insuranceCompany,
                insuranceStartDate = insuranceStartDate,
                insuranceEndDate = insuranceEndDate,
                insuranceEmergencyCall = insuranceEmergencyCall,
                updatedAt = System.currentTimeMillis()
            )
            vehicleRepository.saveVehicle(updated)
            onComplete()
        }
    }

    fun recordInspectionResult(
        car: CarEntity,
        inspectionDate: String,
        inspectionType: String,
        stationName: String?,
        isPassed: Boolean,
        nextExpiryDate: String?,
        retestDeadline: String?,
        cost: Int,
        memo: String?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // 적합 판정 시에만 차기 유효기간 만료일 반영 (부적합 시에는 만료일 연장 금지)
            val calculatedStart = if (isPassed && nextExpiryDate != null) {
                runCatching {
                    java.time.LocalDate.parse(nextExpiryDate).minusDays(90).toString()
                }.getOrNull()
            } else car.inspectionStartDate

            val updatedCar = car.copy(
                inspectionType = inspectionType,
                inspectionStartDate = if (isPassed) calculatedStart else car.inspectionStartDate,
                inspectionEndDate = if (isPassed && nextExpiryDate != null) nextExpiryDate else car.inspectionEndDate,
                isInspectionDateEstimated = false,
                lastInspectionDate = inspectionDate,
                lastInspectionResult = if (isPassed) "적합" else "부적합 (재검사)",
                retestDeadline = if (!isPassed) retestDeadline else null,
                updatedAt = System.currentTimeMillis()
            )

            val historyEntity = maha.garage.data.local.entity.CarInspectionHistoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                carId = car.id,
                inspectionDate = inspectionDate,
                inspectionType = inspectionType,
                stationName = stationName,
                isPassed = isPassed,
                nextExpiryDate = if (isPassed) nextExpiryDate else null,
                retestDeadline = if (!isPassed) retestDeadline else null,
                cost = cost,
                memo = memo,
                certificatePhotoUri = null,
                createdAt = System.currentTimeMillis()
            )

            maintenanceRepository.recordInspectionResult(updatedCar, historyEntity)
            onComplete()
        }
    }
}
