package maha.garage.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import maha.garage.data.local.entity.FuelRecordEntity
import maha.garage.data.model.enums.DueStatus
import maha.garage.domain.engine.FuelCalculator
import maha.garage.domain.model.ItemEvaluation
import maha.garage.domain.model.VehicleWithStatus
import maha.garage.domain.repository.FuelRepository
import maha.garage.domain.repository.VehicleRepository
import java.util.Calendar

data class DashboardUiState(
    val vehiclesWithStatus: List<VehicleWithStatus> = emptyList(),
    val selectedVehicleIndex: Int = 0,
    val pinnedVehicleId: String? = null,
    val isLoading: Boolean = true,
    val fuelRecords: List<FuelRecordEntity> = emptyList(),
    val averageEconomy: Double? = null,
    val recentEconomy: Double? = null,
    val officialEconomy: Double? = null,
    val recentEconomies: List<Double> = emptyList(), // 과거 -> 최신 순
    val monthlyFuelExpense: Int = 0,
    val monthlyFuelAmount: Double = 0.0,
    val costPerKm: Int? = null
) {
    val selectedVehicle: VehicleWithStatus?
        get() = vehiclesWithStatus.getOrNull(selectedVehicleIndex)

    val isCurrentVehiclePinned: Boolean
        get() = selectedVehicle?.car?.id != null && selectedVehicle?.car?.id == pinnedVehicleId

    val totalOverdueCount: Int
        get() = vehiclesWithStatus.sumOf { it.overdueCount }

    val totalDueSoonCount: Int
        get() = vehiclesWithStatus.sumOf { it.dueSoonCount }

    val urgentItems: List<Pair<VehicleWithStatus, ItemEvaluation>>
        get() = vehiclesWithStatus.flatMap { v ->
            v.evaluations
                .filter { it.status == DueStatus.OVERDUE || it.status == DueStatus.DUE_SOON || it.status == DueStatus.DUE_NOW }
                .map { v to it }
        }.sortedBy { (_, item) ->
            when (item.status) {
                DueStatus.OVERDUE -> 0
                DueStatus.DUE_NOW -> 1
                DueStatus.DUE_SOON -> 2
                else -> 3
            }
        }
}

class DashboardViewModel(
    private val vehicleRepository: VehicleRepository,
    private val fuelRepository: FuelRepository,
    private val preferencesStore: maha.garage.data.preferences.FuelPreferencesStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val inMemoryPinnedId = MutableStateFlow<String?>(null)
    private val inMemorySelectedId = MutableStateFlow<String?>(null)

    private var fuelObservationJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        val pinnedFlow = preferencesStore?.pinnedVehicleIdFlow ?: inMemoryPinnedId
        val selectedFlow = preferencesStore?.selectedVehicleIdFlow ?: inMemorySelectedId

        viewModelScope.launch {
            combine(
                vehicleRepository.getAllVehiclesWithStatus(),
                pinnedFlow,
                selectedFlow
            ) { list, pinnedId, selectedId ->
                Triple(list, pinnedId, selectedId)
            }.collect { (list, pinnedId, selectedId) ->
                val activeList = list.filter { !it.car.isArchived }

                // 1. 고정된 차량(Pinned)을 0번 인덱스(가장 왼쪽)에 배치되도록 정렬
                val sortedList = if (!pinnedId.isNullOrBlank()) {
                    val pinned = activeList.filter { it.car.id == pinnedId }
                    val others = activeList.filter { it.car.id != pinnedId }
                    pinned + others
                } else {
                    activeList
                }

                // 2. 선택된 차량 인덱스 결정:
                // - DataStore에 저장된 선택 차량이 존재하면 해당 인덱스
                // - 없으면 고정된 차량(0번 인덱스) 우선 선택
                // - 그 외 이전 인덱스 또는 0번
                val newIndex = when {
                    !selectedId.isNullOrBlank() -> {
                        val idx = sortedList.indexOfFirst { it.car.id == selectedId }
                        if (idx >= 0) idx else 0
                    }
                    !pinnedId.isNullOrBlank() -> 0
                    _uiState.value.selectedVehicleIndex in sortedList.indices -> _uiState.value.selectedVehicleIndex
                    else -> 0
                }

                val currentSelectedCarId = _uiState.value.selectedVehicle?.car?.id
                val newSelectedCar = sortedList.getOrNull(newIndex)?.car

                _uiState.update { current ->
                    current.copy(
                        vehiclesWithStatus = sortedList,
                        selectedVehicleIndex = newIndex,
                        pinnedVehicleId = pinnedId,
                        isLoading = false
                    )
                }

                // 선택 차량이 변경되었거나 최초 로딩 시 주유 데이터 관찰
                if (newSelectedCar != null && (fuelObservationJob == null || currentSelectedCarId != newSelectedCar.id)) {
                    observeFuelRecordsForVehicle(newSelectedCar.id, newSelectedCar.fuelEconomy)
                }
            }
        }
    }

    fun selectVehicle(index: Int) {
        val car = _uiState.value.vehiclesWithStatus.getOrNull(index)?.car ?: return
        _uiState.update { it.copy(selectedVehicleIndex = index) }
        observeFuelRecordsForVehicle(car.id, car.fuelEconomy)

        viewModelScope.launch {
            if (preferencesStore != null) {
                preferencesStore.setSelectedVehicleId(car.id)
            } else {
                inMemorySelectedId.value = car.id
            }
        }
    }

    fun pinAndMoveToFirst(vehicleId: String) {
        val currentList = _uiState.value.vehiclesWithStatus
        val targetVehicle = currentList.find { it.car.id == vehicleId }
        if (targetVehicle != null) {
            val pinned = listOf(targetVehicle)
            val others = currentList.filter { it.car.id != vehicleId }
            val reordered = pinned + others
            _uiState.update { current ->
                current.copy(
                    vehiclesWithStatus = reordered,
                    selectedVehicleIndex = 0,
                    pinnedVehicleId = vehicleId
                )
            }
            observeFuelRecordsForVehicle(targetVehicle.car.id, targetVehicle.car.fuelEconomy)
        }
        viewModelScope.launch {
            if (preferencesStore != null) {
                preferencesStore.setPinnedVehicleId(vehicleId)
                preferencesStore.setSelectedVehicleId(vehicleId)
            } else {
                inMemoryPinnedId.value = vehicleId
                inMemorySelectedId.value = vehicleId
            }
        }
    }

    fun togglePin(vehicleId: String) {
        viewModelScope.launch {
            if (preferencesStore != null) {
                preferencesStore.togglePin(vehicleId)
            } else {
                val current = inMemoryPinnedId.value
                inMemoryPinnedId.value = if (current == vehicleId) null else vehicleId
            }
        }
    }

    fun togglePinCurrentVehicle() {
        val car = _uiState.value.selectedVehicle?.car ?: return
        togglePin(car.id)
    }

    private fun observeFuelRecordsForVehicle(vehicleId: String, officialEconomyStr: String?) {
        fuelObservationJob?.cancel()
        fuelObservationJob = viewModelScope.launch {
            val officialEconomy = officialEconomyStr
                ?.filter { it.isDigit() || it == '.' }
                ?.toDoubleOrNull()

            fuelRepository.getRecordsForVehicle(vehicleId).collect { records ->
                // records: 최신 -> 과거 순
                val recentEco = records.firstOrNull { it.calculatedEconomy != null }?.calculatedEconomy
                val validEconomies = records
                    .mapNotNull { it.calculatedEconomy }
                    .reversed() // 과거 -> 최신 순으로 변환

                val avgEco = if (validEconomies.isNotEmpty()) {
                    validEconomies.average().let { (it * 100.0).toInt() / 100.0 }
                } else null

                // 이번 달 시작 일시
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val monthStart = cal.timeInMillis

                val thisMonthRecords = records.filter { it.refuelDate >= monthStart }
                val monthExpense = thisMonthRecords.sumOf { it.totalPrice }
                val monthAmount = thisMonthRecords.sumOf { it.fuelAmount }

                // 1km당 유류비 계산 (최근 2개 이상 주행거리 차이와 결제액 기준)
                val costKm = if (records.size >= 2) {
                    val latest = records.first()
                    val earliest = records.last()
                    val dist = latest.odometer - earliest.odometer
                    val totalCost = records.subList(0, records.size - 1).sumOf { it.totalPrice }
                    FuelCalculator.calculateCostPerKm(totalCost, dist)
                } else null

                _uiState.update { current ->
                    current.copy(
                        fuelRecords = records,
                        recentEconomy = recentEco,
                        averageEconomy = avgEco,
                        officialEconomy = officialEconomy,
                        recentEconomies = validEconomies.takeLast(10),
                        monthlyFuelExpense = monthExpense,
                        monthlyFuelAmount = (monthAmount * 10.0).toInt() / 10.0,
                        costPerKm = costKm
                    )
                }
            }
        }
    }

    fun addFuelRecord(
        odometer: Int,
        fuelAmount: Double,
        unitPrice: Int,
        totalPrice: Int,
        isFull: Boolean,
        isMissedPrevious: Boolean,
        gasStationName: String?
    ) {
        val car = _uiState.value.selectedVehicle?.car ?: return
        viewModelScope.launch {
            fuelRepository.addFuelRecord(
                vehicleId = car.id,
                odometer = odometer,
                fuelAmount = fuelAmount,
                unitPrice = unitPrice,
                totalPrice = totalPrice,
                isFull = isFull,
                isMissedPrevious = isMissedPrevious,
                gasStationName = gasStationName
            )
        }
    }

    fun deleteFuelRecord(record: FuelRecordEntity) {
        viewModelScope.launch {
            fuelRepository.deleteRecord(record)
        }
    }

    fun updateMileage(
        carId: String,
        newMileage: Int,
        summerTireMileage: Int? = null,
        winterTireMileage: Int? = null,
        lastSwapMileage: Int? = null
    ) {
        viewModelScope.launch {
            vehicleRepository.updateMileage(
                carId = carId,
                newMileage = newMileage,
                summerTireMileage = summerTireMileage,
                winterTireMileage = winterTireMileage,
                lastSwapMileage = lastSwapMileage
            )
        }
    }
}
