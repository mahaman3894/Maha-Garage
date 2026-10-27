package maha.garage.ui.tutorial

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import maha.garage.data.local.DatabaseSeeder
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.data.local.entity.*
import maha.garage.data.model.enums.*
import maha.garage.data.remote.dto.VehicleSpecDto
import maha.garage.data.repository.*
import java.time.LocalDate

data class TutorialCue(val chapter: Int, val page: String, val target: String, val title: String, val body: String, val click: Boolean = true)

/** The same composables and repositories as the app, backed ONLY by a volatile Room database. */
class TutorialSession(val database: MahaGarageDatabase, initialIndex: Int = 0) {
    var index by mutableIntStateOf(initialIndex.coerceIn(0, TutorialCues.lastIndex))
    val cue get() = TutorialCues[index]
    val anchors = mutableStateMapOf<String, TutorialAnchor>()
    val viewModels = mutableListOf<ViewModel>()
    var busy by mutableStateOf(false)
    var feedback by mutableStateOf<String?>(null)
    var pointerScreen: Offset? = null
    val vehicles = VehicleRepositoryImpl(database)
    val maintenance = MaintenanceRepositoryImpl(database)
    val fuel = FuelRepositoryImpl(database.fuelRecordDao(), database.carDao())
    fun close() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        database.close()
    }
    companion object {
        suspend fun create(context: Context, initialIndex: Int = 0): TutorialSession = withContext(Dispatchers.IO) {
            val db = Room.inMemoryDatabaseBuilder(context.applicationContext, MahaGarageDatabase::class.java).build()
            try {
                DatabaseSeeder(db).seedIfNeeded()
                val car = TutorialFixtures.car.copy(currentMileage = if (initialIndex > TutorialCues.indexOfFirst { it.target == "mileage.save" }) 50600 else 50000)
                VehicleRepositoryImpl(db).saveVehicle(car)
                db.tireDao().insertTires(listOf("FL", "FR", "RL", "RR").map {
                    TireEntity(carId = car.id, position = it, assignedWheelPosition = it,
                        season = "SUMMER", installedAtMileage = 50000, isMounted = true)
                })
                val date = LocalDate.now().minusDays(20).toString()
                val visit = ServiceVisitEntity(id = "tutorial-service", carId = car.id, serviceDate = date,
                    odometerKm = 49000, workshop = "예시 정비소", totalCost = 85000, notes = "튜토리얼 예시 · 엔진오일과 오일필터 교체")
                db.serviceVisitDao().recordFullService(visit, listOf(
                    ServiceLineEntity(visitId = visit.id, itemCode = "ENGINE_OIL", actionType = ActionType.REPLACE, partsCost = 75000),
                    ServiceLineEntity(visitId = visit.id, itemCode = "OIL_FILTER", actionType = ActionType.REPLACE, partsCost = 10000)
                ))
                db.maintenanceRuleDao().updateLastReplacement(car.id, "ENGINE_OIL", date, 49000)
                val repo = FuelRepositoryImpl(db.fuelRecordDao(), db.carDao())
                repo.addFuelRecord(car.id, 50000, 50.0, 1700, 85000, true, false, "예시 주유소", System.currentTimeMillis() - 86400000L)
                if (initialIndex >= TutorialCues.indexOfFirst { it.target == "fuel.summary" }) repo.addFuelRecord(car.id, 50600, 50.0, 1700, 85000, true, false, "예시 주유소")
                TutorialSession(db, initialIndex)
            } catch (e: Throwable) { db.close(); throw e }
        }
    }
}

data class TutorialAnchor(val token: Any, val bounds: Rect, val click: () -> Unit, val reveal: suspend () -> Unit)
val LocalTutorial = staticCompositionLocalOf<TutorialSession?> { null }

/** Dialog dimming would darken even the transparent spotlight hole. */
@Composable
fun TutorialUndimWindow() {
    val tutorial = LocalTutorial.current
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
        if (tutorial != null && window != null) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0f)
        }
    }
}

/** Native dialog windows have their own origins. Store physical screen coordinates for ALL targets. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tutorialTarget(id: String, action: () -> Unit = {}): Modifier = composed {
    val session = LocalTutorial.current
    if (session == null) this else {
        TutorialUndimWindow()
        val view = LocalView.current
        val requester = remember { BringIntoViewRequester() }
        val latestAction by rememberUpdatedState(action)
        val token = remember { Any() }
        DisposableEffect(session, id) {
            onDispose { if (session.anchors[id]?.token === token) session.anchors.remove(id) }
        }
        this.bringIntoViewRequester(requester).onGloballyPositioned { coordinates ->
            val origin = IntArray(2)
            view.rootView.getLocationOnScreen(origin)
            // Do not clip to the viewport: an offscreen control must register BEFORE
            // bringIntoView can scroll it into view, especially on short landscape screens.
            val points = listOf(Offset.Zero, Offset(coordinates.size.width.toFloat(), 0f),
                Offset(0f, coordinates.size.height.toFloat()),
                Offset(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()))
                .map { coordinates.localToWindow(it) + Offset(origin[0].toFloat(), origin[1].toFloat()) }
            val rect = Rect(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
            if (rect.width > 1 && rect.height > 1 && session.anchors[id]?.bounds != rect) {
                session.anchors[id] = TutorialAnchor(token, rect, { latestAction() }, { requester.bringIntoView() })
            }
        }
    }
}

object TutorialFixtures {
    const val CAR_ID = "tutorial-only-volatile-car"
    val car = CarEntity(id = CAR_ID, carNumber = "12가3456", modelName = "그랜저 하이브리드", make = "현대",
        nickname = "연습용 차량", powertrain = PowertrainType.HEV_GASOLINE, currentMileage = 50000,
        firstRegistrationDate = "2023-04-10", modelYear = 2023, lastSwapMileage = 50000,
        insuranceCompany = "예시 보험사", insuranceEmergencyCall = "000-0000-0000")
    val spec = VehicleSpecDto("12가3456", "그랜저 하이브리드", "현대", "가솔린 하이브리드", "1,598 cc",
        "2023-04-10", LocalDate.now().plusMonths(6).toString(), "대형 승용", PowertrainType.HEV_GASOLINE, VehicleClass.SEDAN, 2023)
}

val TutorialCues = listOf(
    TutorialCue(1,"vehicles","garage.add","내 차 등록부터 시작해요","실제 차량 추가 버튼을 눌러 등록 화면을 열어볼게요. 연습 데이터는 종료하면 사라집니다."),
    TutorialCue(1,"add","vehicle.catalog","카탈로그로 모델 찾기","카탈로그에서 제조사·모델·트림을 고르면 제원이 자동 입력됩니다."),
    TutorialCue(1,"add","catalog.search","이니셜도 검색할 수 있어요","검색창에 ‘E클래스’를 입력해 볼게요. 영문 대소문자를 구분하지 않습니다."),
    TutorialCue(1,"add","catalog.first","검색 결과를 선택해요","실제 검색 결과를 선택하면 등록 양식에 제원이 채워집니다."),
    TutorialCue(1,"add","vehicle.plate","차량번호 조회안내","차량번호 조회 버튼에서 서비스 제공 상태를 확인할 수 있습니다.\n\n-해당 서비스는 준비중입니다-\n개인정보 수집관련 이슈",false),
    TutorialCue(1,"add","vehicle.save","등록을 마무리해요","저장 버튼의 동작까지 연습합니다. 개인 차량과 실제 정비 데이터에는 영향을 주지 않습니다."),
    TutorialCue(2,"vehicles","garage.car","차량 카드를 선택해 소모품 변경 사항을 등록하세요.","차량 카드를 선택하면 주행거리 확인 후 소모품 변경 사항을 등록할 수 있습니다."),
    TutorialCue(2,"vehicles","mileage.input","계기판의 누적거리를 넣어요","예시로 50,600km를 입력합니다. 트립 거리 대신 계기판의 ODO 값을 사용하세요."),
    TutorialCue(2,"vehicles","mileage.save","주행거리를 갱신해요","저장하면 상세 화면으로 이동하고 소모품의 남은 거리가 다시 계산됩니다."),
    TutorialCue(3,"detail","detail.oil","부품별 교체 시기를 확인해요","실제 엔진오일 박스를 눌러 교체 기록·남은 거리·주기 설정을 살펴봅니다."),
    TutorialCue(3,"detail","consumable.form","정비 기록과 주기 설정","제조사·제품명·점도 등 사용한 부품 정보를 입력하고 교체 기록을 남길 수 있습니다. 교체 기록을 저장하면 다음 교체 시점이 갱신됩니다.",false),
    TutorialCue(4,"dashboard","fuel.add","주유 기록을 추가해요","상단의 + 버튼에서 실제 주유 등록창을 엽니다."),
    TutorialCue(4,"dashboard","fuel.camera","영수증은 카메라 OCR로","등록창의 카메라 버튼을 누르면 영수증 스캔 화면이 열립니다."),
    TutorialCue(4,"dashboard","scanner.capture","영수증을 인식해요","50L · 단가 1,700원 · 합계 85,000원 예시를 인식합니다. 카메라 권한이나 실제 촬영은 사용하지 않습니다."),
    TutorialCue(4,"dashboard","fuel.save","자동 입력값을 확인해요","주유량과 금액을 확인하고 저장합니다. 예시 ODO는 50,600km, 가득 주유입니다."),
    TutorialCue(4,"dashboard","fuel.summary","실제 연비 계산 결과","이전 가득 주유 50,000km에서 600km를 주행하고 50L를 채워 12.00km/L가 계산됩니다.",false),
    TutorialCue(5,"detail","detail.emergency","사고·고장 때는 긴급출동","상세 화면의 고정된 긴급출동 버튼으로 보험사 연락처와 대처 요령을 확인할 수 있습니다."),
    TutorialCue(5,"detail","emergency.phone","보험사 연락처를 확인해요","버튼을 누르면 실제 사용 시 전화 앱으로 연결됩니다. 지금은 전화 연결을 시뮬레이션합니다."),
    TutorialCue(5,"settings","settings.history","카 히스토리로 이동해요","설정의 카 히스토리에서 과거 정비 기록을 한눈에 확인합니다."),
    TutorialCue(5,"history","history.records","모든 준비가 끝났어요","예시 엔진오일·오일필터 교체 이력을 확인하세요. 완료하면 실제 차고로 돌아갑니다. 설정에서 언제든 다시 볼 수 있습니다.",false)
)
