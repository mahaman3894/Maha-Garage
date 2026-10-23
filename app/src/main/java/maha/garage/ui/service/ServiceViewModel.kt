package maha.garage.ui.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import maha.garage.data.local.dao.ServiceVisitWithLines
import maha.garage.data.local.entity.CarEntity
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.local.entity.ServiceLineEntity
import maha.garage.data.local.entity.ServiceVisitEntity
import maha.garage.data.local.entity.TireSwapLogEntity
import maha.garage.domain.repository.MaintenanceRepository
import maha.garage.domain.repository.VehicleRepository

data class ServiceHistoryUiState(
    val cars: List<CarEntity> = emptyList(),
    val selectedCarId: String? = null,
    val visits: List<ServiceVisitWithLines> = emptyList(),
    val swapLogs: List<TireSwapLogEntity> = emptyList(),
    val totalExpense: Long = 0L,
    val isLoading: Boolean = true
)

class ServiceViewModel(
    private val maintenanceRepository: MaintenanceRepository,
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _selectedCarId = MutableStateFlow<String?>(null)
    val selectedCarId: StateFlow<String?> = _selectedCarId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ServiceHistoryUiState> = combine(
        vehicleRepository.getAllActiveVehicles(),
        _selectedCarId.flatMapLatest { id ->
            (if (id == null) maintenanceRepository.getAllVisits() else maintenanceRepository.getVisitsForCar(id)).map { id to it }
        }
    ) { activeCars, selection ->
        ServiceHistoryUiState(cars = activeCars, selectedCarId = selection.first, visits = selection.second,
            totalExpense = selection.second.sumOf { it.visit.totalCost }, isLoading = false)
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000, replayExpirationMillis = 0), ServiceHistoryUiState())

    val catalog: StateFlow<List<MaintenanceCatalogEntity>> = maintenanceRepository.getCatalog()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cars: StateFlow<List<CarEntity>> = vehicleRepository.getAllActiveVehicles()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectCar(carId: String?) { _selectedCarId.value = carId }

    fun loadSwapLogs(carId: String): Flow<List<TireSwapLogEntity>> {
        return maintenanceRepository.getSwapLogsForCar(carId)
    }

    fun recordVisit(
        visit: ServiceVisitEntity,
        lines: List<ServiceLineEntity>,
        onError: (String) -> Unit = {},
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                maintenanceRepository.recordServiceVisit(visit, lines)
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { onError(e.message ?: "정비 기록 저장에 실패했습니다.") }
        }
    }
}
