package maha.garage.domain.engine

import maha.garage.data.local.entity.CarEntity
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass

/**
 * 소모품 부품 교체 이유, 미교체 시 발생하는 문제, 전조 증상, 차종별 맞춤 정비 조언 데이터 모델
 */
data class ConsumableGuideInfo(
    val itemCode: String,
    val title: String,
    val vehicleCategoryLabel: String,
    val intervalText: String,
    val warningSymptoms: List<String>,
    val problemRisks: List<String>,
    val expertAdvice: String,
    val isApplicable: Boolean = true
)

object ConsumableGuideProvider {

    enum class VehicleGuideCategory(val label: String) {
        BEV("전기차 (BEV) 맞춤 가이드"),
        HEV("하이브리드 (HEV) 맞춤 가이드"),
        SUV_DIESEL("SUV · 디젤 사륜 맞춤 가이드"),
        BUS_TRUCK("대형 버스 · 상용차 맞춤 가이드"),
        GASOLINE_SEDAN("가솔린 승용 맞춤 가이드")
    }

    fun resolveCategory(car: CarEntity): VehicleGuideCategory {
        return when {
            car.powertrain == PowertrainType.BEV -> VehicleGuideCategory.BEV
            car.powertrain in listOf(
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL,
                PowertrainType.PHEV_GASOLINE,
                PowertrainType.PHEV_DIESEL
            ) -> VehicleGuideCategory.HEV
            car.vehicleClass in listOf(
                VehicleClass.BUS,
                VehicleClass.HEAVY_TRUCK,
                VehicleClass.MEDIUM_TRUCK,
                VehicleClass.LIGHT_TRUCK
            ) -> VehicleGuideCategory.BUS_TRUCK
            car.vehicleClass == VehicleClass.SUV || car.powertrain == PowertrainType.DIESEL -> VehicleGuideCategory.SUV_DIESEL
            else -> VehicleGuideCategory.GASOLINE_SEDAN
        }
    }

    fun getGuide(car: CarEntity, rawItemCode: String): ConsumableGuideInfo {
        val category = resolveCategory(car)
        val normalizedCode = when (rawItemCode) {
            "BRAKE_PAD_FRONT", "BRAKE_PAD_REAR" -> "BRAKE_PAD"
            "TIMING_BELT_WATER_PUMP" -> "WATER_PUMP"
            "EV_COOLANT" -> "COOLANT"
            "OIL_FILTER" -> "ENGINE_OIL"
            else -> rawItemCode
        }

        return when (category) {
            VehicleGuideCategory.BEV -> getBevGuide(normalizedCode, car)
            VehicleGuideCategory.HEV -> getHevGuide(normalizedCode, car)
            VehicleGuideCategory.SUV_DIESEL -> getSuvDieselGuide(normalizedCode, car)
            VehicleGuideCategory.BUS_TRUCK -> getBusTruckGuide(normalizedCode, car)
            VehicleGuideCategory.GASOLINE_SEDAN -> getGasolineSedanGuide(normalizedCode, car)
        }
    }

    // 1. 전기차 (BEV) 전용 안내
    private fun getBevGuide(itemCode: String, car: CarEntity): ConsumableGuideInfo {
        val label = VehicleGuideCategory.BEV.label
        return when (itemCode) {
            "ENGINE_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "엔진오일 (오일필터 포함)",
                vehicleCategoryLabel = label,
                intervalText = "해당 없음 (엔진 미탑재)",
                warningSymptoms = listOf("전기차는 내연기관 엔진이 없어 엔진오일이 주입되지 않습니다."),
                problemRisks = listOf("교체 및 점검이 불필요한 부품입니다."),
                expertAdvice = "전기차는 모터 구동축의 '감속기 오일'과 '고전압 저전도 냉각수'가 핵심 윤활 및 냉각 품목입니다.",
                isApplicable = false
            )
            "ENGINE_AIR_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어클리너 (흡기 필터)",
                vehicleCategoryLabel = label,
                intervalText = "해당 없음 (엔진 미탑재)",
                warningSymptoms = listOf("공기 흡입 및 연소 폭발 장치가 없으므로 흡기 필터가 장착되지 않습니다."),
                problemRisks = listOf("교체 및 점검이 불필요한 부품입니다."),
                expertAdvice = "실내 쾌적성을 위해 캐빈(에어컨·히터) 필터만 정기적으로 교체해 주시면 됩니다.",
                isApplicable = false
            )
            "TRANSMISSION_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "변속기 오일 (다단 트랜스미션)",
                vehicleCategoryLabel = label,
                intervalText = "해당 없음 (단일 감속기 탑재)",
                warningSymptoms = listOf("단일 기어비 감속기를 사용하므로 다단 변속기 오일이 적용되지 않습니다."),
                problemRisks = listOf("단일 감속기 오일(REDUCTION_GEAR_OIL) 항목으로 관리됩니다."),
                expertAdvice = "전기차는 '감속기 오일' 항목을 통해 점검 및 교환을 진행하세요.",
                isApplicable = false
            )
            "TIMING_BELT_SET", "DRIVE_BELT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "타이밍벨트 / 구동벨트",
                vehicleCategoryLabel = label,
                intervalText = "해당 없음 (엔진 미탑재)",
                warningSymptoms = listOf("모터로 직접 동력을 전달하므로 구동 벨트류가 존재하지 않습니다."),
                problemRisks = listOf("벨트 마모나 끊어짐 위험이 원천적으로 없습니다."),
                expertAdvice = "전동식 컴프레서와 워터펌프 역시 전기 모터로 직접 구동됩니다.",
                isApplicable = false
            )
            "REDUCTION_GEAR_OIL", "DIFFERENTIAL_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "감속기 오일 (모터 리덕션 기어)",
                vehicleCategoryLabel = label,
                intervalText = "매 40,000km ~ 60,000km",
                warningSymptoms = listOf(
                    "가속 페달을 밟거나 놓을 때 하부에서 '위잉-' 하는 모터 감속기 고주파 하울링 소음",
                    "오일 점검 시 짙은 변색 및 쇳가루 분진 다량 검출",
                    "저속 회전 시 미세한 진동 및 동력 전달 딜레이"
                ),
                problemRisks = listOf(
                    "고토크 전기 모터의 순간 출력을 감당하는 감속 기어 치면 마모 및 유격 발생",
                    "기어 파손 시 수백만 원 상당의 통합 드라이브 유닛(모터+인버터+감속기) 전체 교체 필요",
                    "주행 중 감속기 고착으로 인한 구동 불능 위험"
                ),
                expertAdvice = "전기차는 초반 최대 토크가 즉각 발휘되므로 감속기 기어 마모가 일반 차량보다 심합니다. 4~5만km 주기 교환을 강력 권장합니다."
            )
            "COOLANT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "배터리·모터 냉각수 (절연 저전도)",
                vehicleCategoryLabel = label,
                intervalText = "매 60,000km ~ 80,000km 또는 3~4년",
                warningSymptoms = listOf(
                    "급속 충전 중 전방 냉각팬이 비정상적으로 최고속 회전",
                    "배터리 온도 조절 실패로 급속 충전 속도 급감",
                    "클러스터 고전압 배터리 냉각 계통 경고등 점등"
                ),
                problemRisks = listOf(
                    "일반 수돗물이나 일반 부동액 혼유 시 전도율 상승으로 고전압 누전 및 화재 위험",
                    "배터리 팩 냉각 실패로 배터리 셀 영구 열화(수명 급단축) 및 페일세이프 출력 제한",
                    "인버터 및 구동모터 과열로 주행 중 긴급 정차"
                ),
                expertAdvice = "전기차는 일반 부동액을 넣으면 누전 쇼크 위험이 있습니다. 반드시 제조사 공인 절연 저전도 냉각수 규격을 사용하세요."
            )
            "BATTERY_12V" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "12V 저전압 보조 배터리",
                vehicleCategoryLabel = label,
                intervalText = "2년 ~ 3년 주기",
                warningSymptoms = listOf(
                    "스마트키로 문이 열리지 않거나, 화면이 켜지지 않고 시스템 부팅(READY) 불능",
                    "원격 차량 제어 앱 통신 두절 및 상태 업데이트 오류",
                    "계기판 전장 계통 전압 불안정 경고"
                ),
                problemRisks = listOf(
                    "고전압 메인 배터리가 100% 충전되어 있어도 안전 릴레이가 안 붙어 차량 전체 완전 벽돌(먹통) 현상",
                    "비상 점프선 또는 견인 없이는 차량 출발 불가",
                    "주행 중 전장제어 컴퓨터(VCU) 전원 리셋 위험"
                ),
                expertAdvice = "메인 배터리가 아무리 꽉 차 있어도 12V 저전압 배터리가 방전되면 차가 켜지지 않습니다. 2~3년 경과 시 선제 교체가 필수입니다."
            )
            "BRAKE_PAD" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "브레이크 패드 (전륜 / 후륜)",
                vehicleCategoryLabel = label,
                intervalText = "매 80,000km ~ 100,000km 이상 (초장수명)",
                warningSymptoms = listOf(
                    "브레이크 디스크 표면에 붉은 녹이 두껍게 누적",
                    "패드 마모보다 사용 빈도 부족으로 인한 고착/긁힘 소음",
                    "원페달 회생제동 해제 후 비상 제동 시 페달 감각 이질감"
                ),
                problemRisks = listOf(
                    "캘리퍼 슬라이딩 핀 고착으로 패드 편마모 및 디스크 긁힘",
                    "비상 풀 브레이킹 상황에서 디스크 녹으로 인한 초기 제동력 상실",
                    "주차 브레이크(EPB) 체결 불능 위험"
                ),
                expertAdvice = "전기차는 회생제동 덕에 패드가 거의 안 닳지만, 도리어 사용을 안 해서 녹이 슬고 굳습니다. 정기적인 디스크 청소 제동(중립 제동)이 권장됩니다."
            )
            "CABIN_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어컨·히터 필터 (캐빈 필터)",
                vehicleCategoryLabel = label,
                intervalText = "매 10,000km ~ 15,000km 또는 6개월",
                warningSymptoms = listOf(
                    "공조기 가동 시 퀴퀴한 냄새",
                    "송풍구 바람 세기 약화 및 창문 습기 제거 지연",
                    "실내 미세먼지 수치 상승"
                ),
                problemRisks = listOf(
                    "전기차 핵심 공조장치인 히트펌프 및 PTC 히터 열교환기 오염",
                    "공기 유입 저항으로 블로어 모터 전력 소모 증가(주행가능거리 감소)",
                    "호흡기 질환 유발"
                ),
                expertAdvice = "전기차는 히트펌프 효율이 주행거리에 직결됩니다. 환절기마다 활성탄 고급 필터로 교체해 주시면 실내 쾌적도와 연비에 좋습니다."
            )
            "WIPER_BLADE" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "와이퍼 블레이드",
                vehicleCategoryLabel = label,
                intervalText = "6개월 ~ 1년 주기",
                warningSymptoms = listOf("작동 시 소음, 줄눈 잔상 및 물 번짐"),
                problemRisks = listOf(
                    "악천후 시 운전자 시야 미확보",
                    "전방 윈드실드 상단 ADAS 및 자율주행 카메라 시야 왜곡으로 오작동 유발",
                    "앞유리 스크래치 발생"
                ),
                expertAdvice = "자율주행/차선유지 기능이 탑재된 전기차는 카메라 영역의 유리 오염을 방지하기 위해 와이퍼 상태 관리가 더욱 중요합니다."
            )
            "WATER_PUMP" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "전동식 워터펌프 (EWP)",
                vehicleCategoryLabel = label,
                intervalText = "100,000km 점검",
                warningSymptoms = listOf("전동 펌프 고주파 웅웅거림", "배터리 충전 시 냉각수 순환 부족 경고"),
                problemRisks = listOf("배터리 셀 팩 및 인버터 순환 불량으로 출력 제한 발동"),
                expertAdvice = "기계식 벨트 구동이 아닌 전동식 모터 펌프이므로 진단기 스캔을 통한 회전수 점검이 필요합니다."
            )
            else -> getGeneralDefaultGuide(itemCode, label, car)
        }
    }

    // 2. 하이브리드 승용 (HEV / PHEV) 전용 안내
    private fun getHevGuide(itemCode: String, car: CarEntity): ConsumableGuideInfo {
        val label = VehicleGuideCategory.HEV.label
        return when (itemCode) {
            "ENGINE_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "엔진오일 (오일필터 포함)",
                vehicleCategoryLabel = label,
                intervalText = "매 7,000km ~ 10,000km 또는 1년 (저점도 0W-16, 0W-20 필수)",
                warningSymptoms = listOf(
                    "EV 모드에서 가솔린 엔진 개입 시 충격 및 이질감 증가",
                    "잦은 시동-정지(ISG) 반복 중 엔진 진동이 거칠어짐",
                    "오일 점도 저하 및 유류 희석 냄새"
                ),
                problemRisks = listOf(
                    "엔진 온도가 오르지 않은 상태로 가동/정지가 반복되어 수분 및 연료 희석 슬러지 급증",
                    "엔진 실린더 내벽 조기 마모 및 하이브리드 핵심 장점인 연비 급락",
                    "가변 밸브 타이밍(VVT) 제어 오작동"
                ),
                expertAdvice = "하이브리드는 엔진이 차가운 채로 켜졌다 꺼지기를 반복합니다. 초저점도(0W-16, 0W-20) 정품 오일 규격을 반드시 준수하세요."
            )
            "ENGINE_AIR_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어클리너 (흡기 필터)",
                vehicleCategoryLabel = label,
                intervalText = "매 15,000km ~ 20,000km",
                warningSymptoms = listOf(
                    "엔진 전환 가속 구간에서 엔진 회전수만 급격히 솟구침",
                    "필터 주름에 카본 및 먼지 누적"
                ),
                problemRisks = listOf(
                    "흡기 저항으로 엔진 구동 시 배터리 충전 효율 저하",
                    "불완전 연소로 인한 배출가스 증가 및 하이브리드 시스템 부하"
                ),
                expertAdvice = "가솔린 단독 차량보다 엔진 가동 시간이 적어 소모 속도가 다소 완만하나, 엔진오일 2회 교환 시 1회는 필히 교체해야 합니다."
            )
            "COOLANT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "냉각수 (엔진용 + 인버터용 분리 관리)",
                vehicleCategoryLabel = label,
                intervalText = "엔진: 100,000km / 인버터 전용 저전도: 매 60,000km ~ 80,000km",
                warningSymptoms = listOf(
                    "인버터 전용 보조 탱크 냉각수 탁도 저하",
                    "고전압 시스템 경고등 점등 및 EV 모드 진입 제한",
                    "엔진 보조탱크 수위 저하"
                ),
                problemRisks = listOf(
                    "고전압 인버터 및 HPCU 과열로 하이브리드 시스템 정지(차량 먹통)",
                    "인버터 파손 시 수백만 원 대의 단품 교체 비용 지출",
                    "엔진 헤드 가스켓 손상"
                ),
                expertAdvice = "하이브리드는 보닛 안에 냉각수 탱크가 2개(엔진용, 인버터용) 있습니다. 인버터용 탱크에는 반드시 전용 저전도 냉각수를 주입해야 합니다."
            )
            "BATTERY_12V" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "12V 보조 배터리 (보통 AGM 탑재)",
                vehicleCategoryLabel = label,
                intervalText = "3년 ~ 4년 주기",
                warningSymptoms = listOf(
                    "시동 버튼을 눌러도 계기판에 'READY' 표시등이 안 뜸",
                    "스마트키 문 열림 반응 지연, 실내등 깜빡임",
                    "시동 릴레이 '딱딱' 소리만 나고 차량 안 켜짐"
                ),
                problemRisks = listOf(
                    "트렁크/하부의 고전압 메인 배터리가 완충되어 있어도 시동 릴레이가 안 붙어 차량 출발 완전 불능",
                    "하이브리드 메인 컴퓨터(HEV-ECU) 통신 오류 코드 다발",
                    "주차 중 방전으로 긴급 점프 서비스 필수"
                ),
                expertAdvice = "고전압 배터리와 12V 시동 배터리는 별개입니다. 3년이 넘으면 예방 차원에서 12V AGM 배터리를 교체해 주셔야 시동 불능을 방지합니다."
            )
            "BRAKE_PAD" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "브레이크 패드 (전륜 / 후륜)",
                vehicleCategoryLabel = label,
                intervalText = "전륜 60,000~100,000km / 후륜 80,000~120,000km",
                warningSymptoms = listOf(
                    "정차 직전 미세한 쇳소리",
                    "회생제동에서 유압 제동으로 넘어갈 때 울컥거리는 이질감",
                    "디스크 표면 녹 발생"
                ),
                problemRisks = listOf(
                    "패드 마모보다 디스크 녹 발생으로 인한 마찰재 박리",
                    "캘리퍼 고착으로 인한 비상 제동력 저하"
                ),
                expertAdvice = "회생제동 덕분에 수명이 일반차의 2배 이상입니다. 두께가 많이 남아있어도 디스크 녹 발생과 캘리퍼 고착 여부를 함께 점검하세요."
            )
            "TRANSMISSION_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "변속기 오일 (e-CVT / 하이브리드 DCT)",
                vehicleCategoryLabel = label,
                intervalText = "매 60,000km ~ 80,000km",
                warningSymptoms = listOf(
                    "모터 주행에서 가솔린 변속 전환 시 덜컥거림 및 울컥임",
                    "변속 지연 및 가속 슬립 현상"
                ),
                problemRisks = listOf(
                    "변속기 내부에 통합된 트랙션 모터의 냉각/절연 기능 저하",
                    "하이브리드 전용 클러치 팩 슬립 손상"
                ),
                expertAdvice = "하이브리드 미션 내부에는 모터가 내장되어 있어 오일의 절연성과 열관리가 매우 중요합니다. 무교환을 맹신하지 마세요."
            )
            "TIMING_BELT_SET", "DRIVE_BELT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "하이브리드 구동벨트 (HSG 벨트)",
                vehicleCategoryLabel = label,
                intervalText = "매 60,000km ~ 80,000km",
                warningSymptoms = listOf(
                    "엔진 시동 걸리는 순간 '삑-' 하는 장력 부족 슬립 소음",
                    "벨트 표면 미세 잔크랙"
                ),
                problemRisks = listOf(
                    "HSG(하이브리드 시동/발전 제너레이터) 구동 실패로 배터리 충전 불가",
                    "주행 중 고전압 시스템 셧다운으로 도로 한복판 긴급 멈춤"
                ),
                expertAdvice = "하이브리드의 HSG 벨트는 일반차 겉벨트보다 순간 부하를 엄청나게 받습니다. 6~8만km에 오토텐셔너와 함께 교환하세요."
            )
            else -> getGasolineSedanGuide(itemCode, car)
        }
    }

    // 3. SUV 및 디젤 사륜구동 (Diesel AWD) 전용 안내
    private fun getSuvDieselGuide(itemCode: String, car: CarEntity): ConsumableGuideInfo {
        val label = VehicleGuideCategory.SUV_DIESEL.label
        return when (itemCode) {
            "ENGINE_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "엔진오일 (DPF 규격 C2/C3 필수)",
                vehicleCategoryLabel = label,
                intervalText = "매 8,000km ~ 10,000km (가혹조건 5,000km ~ 7,000km)",
                warningSymptoms = listOf(
                    "디젤 특유의 겔겔거리는 소음과 핸들/시트 진동 급증",
                    "DPF(매연저감장치) 포집 재생 주기가 비정상적으로 잦아짐",
                    "오일 게이지 확인 시 경유 유입으로 오일 레벨이 F선 위로 늘어남"
                ),
                problemRisks = listOf(
                    "경유 희석으로 윤활력 상실 시 고가의 터보차저 베어링 고착",
                    "황 성분 오일 사용 시 DPF 매연 필터 막힘 (DPF 교체비 200~300만 원)",
                    "고압축비 디젤 실린더 내벽 스크래치"
                ),
                expertAdvice = "DPF가 장착된 디젤 SUV는 무조건 ACEA C2/C3 규격을 써야 DPF가 안 막힙니다. 오일량이 늘어난다면 즉시 교환해야 합니다."
            )
            "ENGINE_AIR_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어클리너 (흡기 필터)",
                vehicleCategoryLabel = label,
                intervalText = "매 10,000km (비포장/캠핑 주행 시 5,000km 점검)",
                warningSymptoms = listOf(
                    "언덕길 가속 시 굼뜸 및 머플러에서 흑연(검은 연기) 분출",
                    "필터 여과지 흙먼지 오염"
                ),
                problemRisks = listOf(
                    "흡입 공기 부족으로 불완전 연소 발생, DPF 매연 포집량 급증 및 DPF 수명 단축",
                    "터보차저 흡기 임펠러 블레이드 오염 및 손상"
                ),
                expertAdvice = "캠핑이나 비포장도로를 자주 다니는 SUV는 흡기 필터가 먼지로 빨리 막힙니다. 자주 털어주고 주기마다 교체하세요."
            )
            "DIFFERENTIAL_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "디퍼런셜 & TC 오일 (4WD / AWD 필수)",
                vehicleCategoryLabel = label,
                intervalText = "매 40,000km ~ 50,000km (전륜 + TC + 후륜)",
                warningSymptoms = listOf(
                    "유턴이나 좁은 골목 회전 시 차체 하부에서 턱턱 치는 듯한 꺾임 진동",
                    "언덕길이나 고속 주행 시 뒤 차축 구동축 하울링 웅웅거림"
                ),
                problemRisks = listOf(
                    "트랜스퍼케이스(TC) 다판 클러치 마모로 4륜 구동 분배 기능 상실",
                    "디퍼런셜 기어 치면 마모로 데후 앗세이 파손 (수백만 원 지출)"
                ),
                expertAdvice = "사륜구동 SUV는 앞 데후, 뒤 데후, 중간 TC(트랜스퍼케이스) 오일을 세트로 교체해야 4륜 시스템을 오래 유지할 수 있습니다."
            )
            "BATTERY_12V" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "12V 배터리 (ISG용 고용량 AGM)",
                vehicleCategoryLabel = label,
                intervalText = "3년 ~ 4년 주기 (보통 80Ah~105Ah AGM 탑재)",
                warningSymptoms = listOf(
                    "정차 시 엔진이 자동으로 꺼지는 ISG(오토 스탑)가 작동 안 함",
                    "아침 첫 시동 모터 구동음이 힘겨워짐"
                ),
                problemRisks = listOf(
                    "고압축비 디젤 엔진의 겨울철 급격한 시동 불능 방전",
                    "예열 플러그 전력 부족으로 한파 시동 실패"
                ),
                expertAdvice = "ISG가 적용된 디젤 SUV에 일반 배터리를 넣으면 수명이 반토막 납니다. 반드시 AGM 규격 배터리로 교체하세요."
            )
            "BRAKE_PAD" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "브레이크 패드 (전륜 / 후륜)",
                vehicleCategoryLabel = label,
                intervalText = "전륜 매 25,000km ~ 35,000km (승용 대비 빠른 마모)",
                warningSymptoms = listOf(
                    "제동 시 밀림 현상, 고속 제동 시 핸들 진동 및 떨림",
                    "휠 주변에 검은 분진 다량 누적"
                ),
                problemRisks = listOf(
                    "공차중량이 2톤에 달하는 SUV 하중으로 디스크 로터 열변형 발생",
                    "내리막길 페이드 현상으로 제동거리 급증 추돌 위험"
                ),
                expertAdvice = "SUV는 차가 무거워서 브레이크 패드가 승용차보다 30% 이상 빨리 닳습니다. 3만km마다 잔여 두께를 실측 점검하세요."
            )
            "TIMING_BELT_SET", "DRIVE_BELT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "외부 구동벨트 세트 (겉벨트+텐셔너+댐퍼풀리)",
                vehicleCategoryLabel = label,
                intervalText = "매 80,000km ~ 100,000km",
                warningSymptoms = listOf(
                    "아침 첫 시동 시 쇠 긁는 소리 또는 찌르륵거리는 귀뚜라미 소리",
                    "댐퍼풀리 고무 부시 분리 및 진동 심화"
                ),
                problemRisks = listOf(
                    "디젤 특유의 높은 토크로 댐퍼풀리 파손 시 발전기, 에어컨, 파워스티어링 즉각 정지",
                    "고속도로 주행 중 스티어링 잠김 및 갓길 멈춤 위험"
                ),
                expertAdvice = "디젤 SUV는 겉벨트만 갈면 안 되고, 반드시 진동을 흡수하는 댐퍼풀리와 아이들러 텐셔너를 세트로 일괄 교환해야 합니다."
            )
            "WIPER_BLADE" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "와이퍼 블레이드 (리어 와이퍼 포함)",
                vehicleCategoryLabel = label,
                intervalText = "6개월 ~ 1년 (앞 2개 + SUV 전용 리어 와이퍼)",
                warningSymptoms = listOf("후방 와류 먼지로 뒤 유리 닦을 때 소음 및 줄 생김"),
                problemRisks = listOf("악천후 시 후방 룸미러 시야 불능 및 뒤 유리 스크래치"),
                expertAdvice = "SUV는 후방 와류로 뒷유리에 흙먼지가 많이 낍니다. 앞 와이퍼 교체 시 리어 와이퍼도 함께 교체해 주세요."
            )
            else -> getGasolineSedanGuide(itemCode, car)
        }
    }

    // 4. 대형 버스 및 상용차 (BUS / TRUCK) 전용 안내
    private fun getBusTruckGuide(itemCode: String, car: CarEntity): ConsumableGuideInfo {
        val label = VehicleGuideCategory.BUS_TRUCK.label
        return when (itemCode) {
            "ENGINE_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "상용 대형 디젤 엔진오일",
                vehicleCategoryLabel = label,
                intervalText = "매 10,000km ~ 15,000km (가혹/시내 정차 잦은 조건)",
                warningSymptoms = listOf(
                    "승객/화물 적재 시 등판 가속 답답함",
                    "매연 배출 증가 및 DPF 재생 주기 빈번",
                    "오일 점도 급저하"
                ),
                problemRisks = listOf(
                    "대형 고마력 디젤 엔진의 피스톤 링 고착 및 터보차저 파손",
                    "DPF 및 SCR 후처리 장치 막힘으로 수백만 원 대의 정비 손실 및 운행 정지"
                ),
                expertAdvice = "잦은 승하차와 공회전이 반복되는 상용차는 오일 전단이 빠릅니다. 대형 상용 전용 고부하 규격 오일을 사용하세요."
            )
            "BRAKE_PAD" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어브레이크 패드 / 라이닝 슈",
                vehicleCategoryLabel = label,
                intervalText = "매 30,000km ~ 50,000km",
                warningSymptoms = listOf(
                    "제동 시 에어 배출음 불규칙 및 제동 밀림",
                    "브레이크 드럼/디스크 과열 및 라이닝 마모 한계"
                ),
                problemRisks = listOf(
                    "승객/화물 적재 시 막대한 운동에너지를 흡수하지 못해 베이퍼 록/페이드로 대형 참사 직결",
                    "에어 브레이크 챔버 고착"
                ),
                expertAdvice = "상용차의 제동력 상실은 대형 사고로 이어집니다. 라이닝 두께와 에어 드라이어 수분 필터를 상시 점검하세요."
            )
            "BATTERY_12V" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "24V 상용 배터리 (직렬 2구)",
                vehicleCategoryLabel = label,
                intervalText = "2년 ~ 3년 주기 (150Ah ~ 200Ah)",
                warningSymptoms = listOf(
                    "도어 개폐 및 전장품 사용 중 전압 강하",
                    "시동 모터 크랭킹 회전 둔화"
                ),
                problemRisks = listOf(
                    "출차 전 시동 불능으로 노선 운행 결행 및 운행 차질",
                    "직렬 2개 배터리 간 불균형으로 과충전 화재 위험"
                ),
                expertAdvice = "24V 계통은 2개 배터리의 용량이 일치해야 합니다. 한쪽만 교체하지 마시고 2개를 동시에 신품으로 일괄 교환하세요."
            )
            "COOLANT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "대용량 상용 냉각수 (리타더 방열 포함)",
                vehicleCategoryLabel = label,
                intervalText = "매 80,000km ~ 100,000km",
                warningSymptoms = listOf("언덕길 등판 시 수온계 급상승, 리타더 보조 제동 효율 저하"),
                problemRisks = listOf("실린더 라이너 캐비테이션 침식 및 대형 엔진 오버히트"),
                expertAdvice = "보조 제동장치인 리타더의 열량이 냉각수로 유입되므로, 냉각수 농도와 방열 성능 유지가 필수적입니다."
            )
            else -> getSuvDieselGuide(itemCode, car)
        }
    }

    // 5. 가솔린 승용 (Gasoline Sedan) 기준 기본 안내
    private fun getGasolineSedanGuide(itemCode: String, car: CarEntity): ConsumableGuideInfo {
        val label = VehicleGuideCategory.GASOLINE_SEDAN.label
        return when (itemCode) {
            "ENGINE_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "엔진오일 (오일필터 포함)",
                vehicleCategoryLabel = label,
                intervalText = "매 7,000km ~ 10,000km 또는 1년 (가혹조건 5,000km ~ 7,000km)",
                warningSymptoms = listOf(
                    "엔진 가속 시 소음 및 잔진동 증가",
                    "오일 게이지 확인 시 짙은 갈색/검은색 변색 및 점도 묽어짐",
                    "연비 저하 및 가속 반응 둔화"
                ),
                problemRisks = listOf(
                    "슬러지 퇴적으로 인한 엔진 오일 라인 막힘",
                    "피스톤 및 실린더 내벽 마모로 엔진 보링 또는 파손(시저) 위험",
                    "수백만 원 대의 엔진 어셈블리 교체 비용 발생"
                ),
                expertAdvice = "도심 단거리 주행, 신호 대기 잦음, 잦은 공회전은 모두 '가혹 조건'에 해당합니다. 7,000km 주기로 교환하는 것이 가장 안전합니다."
            )
            "ENGINE_AIR_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어클리너 (흡기 필터)",
                vehicleCategoryLabel = label,
                intervalText = "매 10,000km ~ 15,000km (엔진오일 교체 시 동시 점검/교체)",
                warningSymptoms = listOf(
                    "필터 주름 사이에 회색/검은 먼지와 이물질 누적",
                    "고속 주행 시 가속 답답함 및 출력 부족"
                ),
                problemRisks = listOf(
                    "흡입 공기량 부족으로 불완전 연소 및 매연 발생",
                    "연비 급감 및 흡기 센서(MAF 센서) 오염 및 고장"
                ),
                expertAdvice = "엔진오일 교환 시 오일필터, 에어클리너와 함께 '엔진 3종 세트'로 교환하시면 엔진 수명을 최상으로 유지할 수 있습니다."
            )
            "WIPER_BLADE" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "와이퍼 블레이드",
                vehicleCategoryLabel = label,
                intervalText = "6개월 ~ 1년 (유리에 줄 잔상이 생길 때)",
                warningSymptoms = listOf(
                    "작동 시 '드르륵' 하는 채터링 소음 발생",
                    "닦아낸 후 줄 형태 잔상이나 물 번짐 현상 잔류"
                ),
                problemRisks = listOf(
                    "폭우 또는 야간 주행 시 전방 시야 미확보로 인한 충돌 위험",
                    "닳아버린 블레이드 프레임이 앞유리를 긁어 영구 스크래치 유발"
                ),
                expertAdvice = "여름철 고온 직사광선과 겨울철 결빙으로 고무 날이 쉽게 변형됩니다. 6개월~1년 주기로 날을 교체해 주세요."
            )
            "CABIN_FILTER" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "에어컨·히터 필터 (캐빈 필터)",
                vehicleCategoryLabel = label,
                intervalText = "매 10,000km 또는 6개월 (환절기 봄/가을 권장)",
                warningSymptoms = listOf(
                    "공조기 작동 시 퀴퀴한 곰팡이 및 식초 냄새 발생",
                    "풍량(바람 세기)이 눈에 띄게 약해짐"
                ),
                problemRisks = listOf(
                    "세균, 곰팡이 포자, 초미세먼지 차내 유입으로 호흡기 질환 유발",
                    "블로어 모터 및 증발기(에바포레이터) 오염 및 과부하"
                ),
                expertAdvice = "황사와 송화가루가 날리는 봄철, 에어컨 사용 전 가을철 등 연 2회 교환하시면 항상 쾌적한 실내 공기를 유지할 수 있습니다."
            )
            "COOLANT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "냉각수 (부동액)",
                vehicleCategoryLabel = label,
                intervalText = "최초 100,000km ~ 200,000km (5~10년), 이후 매 40,000km ~ 50,000km (2년)",
                warningSymptoms = listOf(
                    "보조 탱크 수위 저하 (MIN 이하)",
                    "냉각수 색상이 탁해지거나 녹물빛(갈색) 변색",
                    "히터 작동 시 단내(달콤한 화학 냄새) 발생"
                ),
                problemRisks = listOf(
                    "엔진 과열(오버히트)로 실린더 헤드 변형 및 가스켓 파손",
                    "라디에이터 및 냉각 라인 내부 부식 및 누수"
                ),
                expertAdvice = "수돗물 외에 지하수나 미네랄워터는 부식을 유발하므로 절대 섞으면 안 됩니다. 규격 부동액으로 교체하세요."
            )
            "TRANSMISSION_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "변속기(미션) 오일",
                vehicleCategoryLabel = label,
                intervalText = "매 60,000km ~ 80,000km (무교환 표기 차량도 8만~10만km 권장)",
                warningSymptoms = listOf(
                    "변속 시 '쿵' 하는 변속 충격 발생",
                    "기어 변경 딜레이 및 특정 단수에서 슬립(알피엠만 오르고 차가 안 나감)"
                ),
                problemRisks = listOf(
                    "밸브바디 막힘 및 클러치 팩 슬립 손상",
                    "수백만 원 상당의 미션 전체 오버홀 또는 교체 필요"
                ),
                expertAdvice = "제조사에서 '무교환'이라고 안내하더라도 도심 주행 가혹조건에서는 8만~10만km 주기로 교환해야 변속 충격을 방지합니다."
            )
            "BATTERY_12V" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "12V 시동 배터리",
                vehicleCategoryLabel = label,
                intervalText = "3년 ~ 4년 또는 50,000km",
                warningSymptoms = listOf(
                    "시동 시 모터 회전이 느리고 힘없이 '겔겔'거림",
                    "야간 전조등 밝기 저하, 블랙박스 상시 녹화 시간 급감"
                ),
                problemRisks = listOf(
                    "겨울철 및 장기 주차 시 시동 불능 방전",
                    "전압 불안정으로 전자제어장치(ECU/TCU) 통신 오류 유발"
                ),
                expertAdvice = "배터리는 3년이 지나면 겨울철 한파에 급격히 성능이 저하됩니다. 3년 차 도래 시 전압 점검 후 선제 교체를 권장합니다."
            )
            "BRAKE_PAD" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "브레이크 패드 (전륜 / 후륜)",
                vehicleCategoryLabel = label,
                intervalText = "전륜 30,000~40,000km / 후륜 50,000~70,000km (잔여 3mm 이하 즉시 교체)",
                warningSymptoms = listOf(
                    "제동 시 '끼익-' 하는 금속 인디케이터 마찰음",
                    "브레이크 페달 밟는 깊이가 깊어지고 제동 거리 증가"
                ),
                problemRisks = listOf(
                    "마찰재 소진으로 쇠 백플레이트가 브레이크 디스크를 파괴적 손상",
                    "제동력 상실(베이퍼 록 및 페이드 현상)로 대형 사고 위험"
                ),
                expertAdvice = "엔진 하중이 실리는 전륜 패드가 후륜보다 1.5~2배 빠르게 마모됩니다. 잔여 두께가 3mm 이하면 즉각 교체해야 합니다."
            )
            "SPARK_PLUG" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "점화플러그 세트",
                vehicleCategoryLabel = label,
                intervalText = "매 80,000km ~ 100,000km (이리듐/백금 기준)",
                warningSymptoms = listOf(
                    "신호 대기 정차 중 차체 덜덜 떨림(부조/찜빠 현상)",
                    "가속 시 버벅거림 및 연비 급감"
                ),
                problemRisks = listOf(
                    "실화(Misfire) 발생 시 미연소 연료가 배기 라인으로 흘러 100만 원 상당 삼원촉매 변환기 파손",
                    "엔진 시동 불능"
                ),
                expertAdvice = "점화플러그 교체 시 점화코일도 함께 점검하여 일괄 교환하면 중복 공임비를 아낄 수 있습니다."
            )
            "IGNITION_COIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "점화코일",
                vehicleCategoryLabel = label,
                intervalText = "매 100,000km 점검 및 교체",
                warningSymptoms = listOf(
                    "급가속 시 엔진 노킹 및 덜컥거림",
                    "엔진 체크 경고등 점등 (특정 기통 실화 코드)"
                ),
                problemRisks = listOf(
                    "한 기통이 완전히 죽으면 엔진 출력 반토막 및 진동 격화",
                    "촉매 손상 및 엔진 헤드 가스켓 손상"
                ),
                expertAdvice = "점화플러그와 함께 10만km 전후에 4개(또는 6개) 전 기통 세트 일괄 교환을 추천합니다."
            )
            "DIFFERENTIAL_OIL" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "디퍼런셜 오일 (후륜/사륜)",
                vehicleCategoryLabel = label,
                intervalText = "매 40,000km ~ 60,000km",
                warningSymptoms = listOf(
                    "코너링이나 유턴 시 뒤쪽 하부에서 턱턱 걸리는 진동",
                    "고속 주행 시 뒤쪽 하부 웅웅거리는 헬리콥터 소음(하울링)"
                ),
                problemRisks = listOf("기어 치면 마모 및 디퍼런셜 기어 파손, 주행 중 뒷바퀴 잠김(락업) 위험"),
                expertAdvice = "후륜구동이나 4륜 승용차는 디퍼런셜 오일을 제때 교체해야 뒤 차축 소음이 발생하지 않습니다."
            )
            "TIMING_BELT_SET", "DRIVE_BELT" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "외부 구동벨트 세트 (겉벨트)",
                vehicleCategoryLabel = label,
                intervalText = "매 60,000km ~ 80,000km",
                warningSymptoms = listOf(
                    "냉간 첫 시동 시 '끼리릭-' 하는 고주파 벨트 슬립 소음",
                    "벨트 안쪽 고무 표면 잔균열(크랙)"
                ),
                problemRisks = listOf(
                    "벨트 파손 시 알터네이터(발전기) 정지로 배터리 방전 및 시동 꺼짐",
                    "파워스티어링 및 워터펌프 정지로 엔진 오버히트"
                ),
                expertAdvice = "시동 걸 때 찌르륵 소리가 난다면 텐셔너 장력 부족 또는 벨트 노후화 증상입니다. 즉시 점검받으세요."
            )
            "WATER_PUMP" -> ConsumableGuideInfo(
                itemCode = itemCode,
                title = "워터펌프",
                vehicleCategoryLabel = label,
                intervalText = "매 100,000km ~ 120,000km (구동벨트 정비 시 동시 작업)",
                warningSymptoms = listOf(
                    "보닛 앞쪽에서 '스르륵', '갈갈'거리는 베어링 마찰 소음",
                    "워터펌프 숨구멍 부근 핑크/녹색 냉각수 누수 자국"
                ),
                problemRisks = listOf(
                    "냉각수 순환 정지로 엔진 급격한 오버히트",
                    "베어링 파손으로 구동벨트 이탈"
                ),
                expertAdvice = "구동벨트 교체 시 워터펌프와 부동액을 함께 교환하면 공임을 크게 절약할 수 있습니다."
            )
            else -> getGeneralDefaultGuide(itemCode, label, car)
        }
    }

    private fun getGeneralDefaultGuide(itemCode: String, label: String, car: CarEntity): ConsumableGuideInfo {
        return ConsumableGuideInfo(
            itemCode = itemCode,
            title = "소모품 정기 점검",
            vehicleCategoryLabel = label,
            intervalText = "제조사 권장 정기 점검 주기",
            warningSymptoms = listOf("주행 성능 저하 및 이상 소음 발생", "정기 점검 주기 도래"),
            problemRisks = listOf("장기간 방치 시 연관 부품의 연쇄 파손 및 고액의 수리비 발생 위험"),
            expertAdvice = "차량 매뉴얼 및 정비소 권장 기준에 맞춰 정기적인 점검 및 교환을 진행해 주세요."
        )
    }
}
