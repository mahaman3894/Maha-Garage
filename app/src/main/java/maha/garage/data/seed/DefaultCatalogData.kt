package maha.garage.data.seed

import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.model.enums.ActionType
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.RulePriority
import maha.garage.data.model.enums.VehicleClass

object DefaultCatalogData {
    val items: List<MaintenanceCatalogEntity> = listOf(
        // ==================== P1: 최우선 정기 소모품 ====================
        MaintenanceCatalogEntity(
            itemCode = "ENGINE_OIL",
            displayName = "엔진오일",
            category = "엔진/구동",
            priority = RulePriority.P1,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 10000,
            defaultIntervalMonths = 12,
            severeIntervalKm = 5000,
            severeIntervalMonths = 6,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL,
                PowertrainType.PHEV_GASOLINE,
                PowertrainType.PHEV_DIESEL
            ),
            guideNotes = "엔진 마모 방지의 핵심. 터보/직분사 차량 및 시내 단거리 주행 시 7,500km 조기 교체 권장."
        ),
        MaintenanceCatalogEntity(
            itemCode = "OIL_FILTER",
            displayName = "오일필터",
            category = "엔진/구동",
            priority = RulePriority.P1,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 10000,
            defaultIntervalMonths = 12,
            severeIntervalKm = 5000,
            severeIntervalMonths = 6,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL,
                PowertrainType.PHEV_GASOLINE,
                PowertrainType.PHEV_DIESEL
            ),
            guideNotes = "엔진오일 교환 시 동시 교체 권장. 이물질 여과 및 유압 유지."
        ),
        MaintenanceCatalogEntity(
            itemCode = "CABIN_FILTER",
            displayName = "에어컨 / 히터 필터",
            category = "필터/공조",
            priority = RulePriority.P1,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 15000,
            defaultIntervalMonths = 12,
            severeIntervalKm = 7500,
            severeIntervalMonths = 6,
            guideNotes = "실내 미세먼지 및 악취 방지. 봄철 꽃가루 및 환절기 주기 교체 권장."
        ),
        MaintenanceCatalogEntity(
            itemCode = "ENGINE_AIR_FILTER",
            displayName = "엔진 흡기 에어크리너",
            category = "엔진/구동",
            priority = RulePriority.P1,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 20000,
            defaultIntervalMonths = 12,
            severeIntervalKm = 15000,
            severeIntervalMonths = 12,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL,
                PowertrainType.PHEV_GASOLINE,
                PowertrainType.PHEV_DIESEL
            ),
            guideNotes = "엔진 흡기 에어크리너(에어필터). 표시 주기: 15,000 ~ 20,000 km (15,000km부터 점검 및 교체 권장)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "WASHER_FLUID",
            displayName = "워셔액",
            category = "기타/소모품",
            priority = RulePriority.P1,
            defaultActionType = ActionType.REFILL,
            isConditionBased = true,
            guideNotes = "부족 경고등 점등 시 에탄올 워셔액으로 보충."
        ),

        // ==================== P2: 주행 체감 및 공조/제동 ====================
        MaintenanceCatalogEntity(
            itemCode = "WIPER_BLADE",
            displayName = "와이퍼 블레이드",
            category = "외장/안전",
            priority = RulePriority.P2,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 10000,
            defaultIntervalMonths = 12,
            guideNotes = "유리 닦임 불량, 줄 생김 또는 소음 발생 시 즉시 교체."
        ),
        MaintenanceCatalogEntity(
            itemCode = "BRAKE_PAD_FRONT",
            displayName = "전륜 브레이크 패드",
            category = "제동계통",
            priority = RulePriority.P2,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 40000,
            defaultIntervalMonths = 24,
            severeIntervalKm = 30000,
            severeIntervalMonths = 18,
            isConditionBased = true,
            guideNotes = "전륜(앞) 패드. 표시 주기: 30,000 ~ 40,000 km (3만km부터 점검 및 마모 한계선 3mm 이하 시 교체)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "BRAKE_PAD_REAR",
            displayName = "후륜 브레이크 패드",
            category = "제동계통",
            priority = RulePriority.P2,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 70000,
            defaultIntervalMonths = 36,
            severeIntervalKm = 50000,
            severeIntervalMonths = 24,
            isConditionBased = true,
            guideNotes = "후륜(뒤) 패드. 표시 주기: 50,000 ~ 70,000 km (5만km부터 점검 및 EPB 전용 진단 모드로 교환)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "BRAKE_FLUID",
            displayName = "브레이크액 (DOT4)",
            category = "제동계통",
            priority = RulePriority.P2,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 40000,
            defaultIntervalMonths = 24,
            guideNotes = "수분 함유량 3% 초과 시 베이퍼 록 위험. 안전 직결 핵심 항목."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TIRE_REPLACEMENT",
            displayName = "타이어 신규 교체",
            category = "타이어/휠",
            priority = RulePriority.P2,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 50000,
            defaultIntervalMonths = 48,
            isConditionBased = true,
            guideNotes = "마모 한계선(1.6mm) 및 제조 후 5년 경과 시 안전을 위해 신규 타이어 교체 권장."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TIRE_ROTATION",
            displayName = "타이어 위치교환",
            category = "타이어/휠",
            priority = RulePriority.P2,
            defaultActionType = ActionType.ROTATE,
            defaultIntervalKm = 10000,
            defaultIntervalMonths = 12,
            guideNotes = "편마모 방지 및 4륜 트레드 균등 마모 유도 (방향성 타이어 주의)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TIRE_SWAP",
            displayName = "섬머 / 윈터 타이어 스왑",
            category = "타이어/휠",
            priority = RulePriority.P2,
            defaultActionType = ActionType.SWAP,
            defaultIntervalMonths = 6,
            isConditionBased = true,
            guideNotes = "기온 7℃ 이하 기준 11월경 윈터 장착, 3월경 섬머 장착 스왑 권장."
        ),

        // ==================== P3: 구동 및 점화 계통 ====================
        MaintenanceCatalogEntity(
            itemCode = "SPARK_PLUG",
            displayName = "점화플러그 세트",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 80000,
            defaultIntervalMonths = 48,
            firstIntervalKm = 100000,
            firstIntervalMonths = 60,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.PHEV_GASOLINE
            ),
            guideNotes = "이리듐/백금 기준 80,000~100,000km, 일반 니켈은 40,000km 교체."
        ),
        MaintenanceCatalogEntity(
            itemCode = "IGNITION_COIL",
            displayName = "점화코일 세트",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 100000,
            defaultIntervalMonths = 60,
            severeIntervalKm = 80000,
            severeIntervalMonths = 48,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.PHEV_GASOLINE
            ),
            isConditionBased = false,
            guideNotes = "점화플러그 2회 교환 시(약 100,000km) 동시 교환 권장."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TRANSMISSION_OIL",
            displayName = "미션오일 (ATF/DCTF/CVTF)",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 80000,
            defaultIntervalMonths = 48,
            severeIntervalKm = 50000,
            severeIntervalMonths = 36,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL,
                PowertrainType.PHEV_GASOLINE,
                PowertrainType.PHEV_DIESEL
            ),
            guideNotes = "무교환 표기 차량도 8~10만km 또는 5년 주기 교환 시 변속 충격 예방."
        ),
        MaintenanceCatalogEntity(
            itemCode = "REDUCTION_GEAR_OIL",
            displayName = "전기차 감속기 오일",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 60000,
            defaultIntervalMonths = 48,
            applicablePowertrains = listOf(PowertrainType.BEV),
            guideNotes = "전기모터 구동 감속기 윤활유. 6만km 점검 및 필요 시 교체."
        ),
        MaintenanceCatalogEntity(
            itemCode = "FUEL_FILTER",
            displayName = "연료필터 카트리지",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 40000,
            defaultIntervalMonths = 24,
            applicablePowertrains = listOf(PowertrainType.DIESEL, PowertrainType.HEV_DIESEL),
            guideNotes = "디젤 고압 인젝터 보호용. 겨울철 시동성 확보를 위해 3~4만km 교체 필수."
        ),
        MaintenanceCatalogEntity(
            itemCode = "WATER_SEPARATOR",
            displayName = "연료 수분분리기 점검",
            category = "엔진/구동",
            priority = RulePriority.P3,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 20000,
            defaultIntervalMonths = 12,
            applicablePowertrains = listOf(PowertrainType.DIESEL, PowertrainType.HEV_DIESEL),
            guideNotes = "디젤 연료라인 수분 유입 경고 시 즉시 드레인 및 필터 점검."
        ),

        // ==================== P4: 주기적 교체/내구 부품 ====================
        MaintenanceCatalogEntity(
            itemCode = "BATTERY_12V",
            displayName = "12V 시동 배터리 (AGM/일반)",
            category = "전장/배터리",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 60000,
            defaultIntervalMonths = 48,
            severeIntervalKm = 50000,
            severeIntervalMonths = 36,
            isConditionBased = false,
            guideNotes = "차량 시동 및 전장 배터리. 표시 주기: 주행거리 50,000 ~ 60,000 km / 기간 기준(더 중요): 3년 ~ 4년 (36~48개월)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "COOLANT",
            displayName = "냉각수 / 부동액",
            category = "엔진/구동",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 40000,
            defaultIntervalMonths = 24,
            firstIntervalKm = 100000,
            firstIntervalMonths = 60,
            guideNotes = "최초 출고 시 100,000km/60개월, 이후 40,000~60,000km/24개월 주기 교체."
        ),
        MaintenanceCatalogEntity(
            itemCode = "EV_COOLANT",
            displayName = "전기차 전용 저전도도 냉각수",
            category = "엔진/구동",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 40000,
            defaultIntervalMonths = 24,
            firstIntervalKm = 100000,
            firstIntervalMonths = 60,
            applicablePowertrains = listOf(PowertrainType.BEV, PowertrainType.PHEV_GASOLINE),
            guideNotes = "고전압 배터리 절연 안전을 위한 전용 저전도도 냉각수 사용 필수 (최초 10만km/5년 후 4만km 주기)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TIMING_BELT_SET",
            displayName = "타이밍벨트(겉벨트)",
            category = "엔진/구동",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 100000,
            defaultIntervalMonths = 60,
            severeIntervalKm = 80000,
            severeIntervalMonths = 48,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL
            ),
            guideNotes = "구동벨트(겉벨트) 및 오토텐셔너. 표시 주기: 80,000 ~ 100,000 km (8만km부터 옐로우 알림)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "TIMING_BELT_WATER_PUMP",
            displayName = "타이밍벨트(워터펌프)",
            category = "엔진/구동",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 130000,
            defaultIntervalMonths = 72,
            severeIntervalKm = 100000,
            severeIntervalMonths = 60,
            applicablePowertrains = listOf(
                PowertrainType.GASOLINE,
                PowertrainType.DIESEL,
                PowertrainType.LPG,
                PowertrainType.HEV_GASOLINE,
                PowertrainType.HEV_DIESEL
            ),
            guideNotes = "타이밍벨트 세트 및 워터펌프, 냉각 순환 계통. 표시 주기: 100,000 ~ 130,000 km (10만km부터 옐로우 알림)."
        ),
        MaintenanceCatalogEntity(
            itemCode = "HSG_BELT",
            displayName = "하이브리드 구동벨트 (HSG)",
            category = "엔진/구동",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 30000,
            defaultIntervalMonths = 24,
            firstIntervalKm = 60000,
            firstIntervalMonths = 48,
            applicablePowertrains = listOf(PowertrainType.HEV_GASOLINE, PowertrainType.PHEV_GASOLINE),
            guideNotes = "하이브리드 스타터-제너레이터 전용 고장력 벨트 (최초 6만km/4년 후 3만km 점검/교체)."
        ),

        // ==================== P5: 점검 및 섀시 세이프티 ====================
        MaintenanceCatalogEntity(
            itemCode = "WHEEL_ALIGNMENT",
            displayName = "휠 얼라인먼트 점검",
            category = "타이어/휠",
            priority = RulePriority.P5,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 20000,
            defaultIntervalMonths = 12,
            guideNotes = "차량 쏠림, 핸들 틀어짐 또는 신품 타이어 교체 시 필수 점검."
        ),
        MaintenanceCatalogEntity(
            itemCode = "BRAKE_DISC",
            displayName = "브레이크 디스크 로터",
            category = "제동계통",
            priority = RulePriority.P5,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 60000,
            isConditionBased = true,
            guideNotes = "제동 시 핸들 떨림, 런아웃 오차 또는 마모 한계선 도달 시 연마/교체."
        ),
        MaintenanceCatalogEntity(
            itemCode = "SUSPENSION_BUSHING",
            displayName = "로워암/부싱 및 쇼크업소버",
            category = "섀시/조향",
            priority = RulePriority.P5,
            defaultActionType = ActionType.INSPECT,
            defaultIntervalKm = 40000,
            isConditionBased = true,
            guideNotes = "과속방지턱 통과 시 찌걱거림 소음, 오일 누유 및 고무 크랙 점검."
        ),
        MaintenanceCatalogEntity(
            itemCode = "DIFFERENTIAL_OIL",
            displayName = "디퍼렌셜(차동기어) / TC 오일",
            category = "엔진/구동",
            priority = RulePriority.P5,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 50000,
            defaultIntervalMonths = 36,
            guideNotes = "후륜구동, 4륜구동(AWD) 및 트럭 구동축 차동기어 오일."
        ),

        // ==================== 상용 트럭 / 대형 오버레이 ====================
        MaintenanceCatalogEntity(
            itemCode = "AIR_DRYER_CARTRIDGE",
            displayName = "에어드라이어 카트리지",
            category = "상용/공압",
            priority = RulePriority.P4,
            defaultActionType = ActionType.REPLACE,
            defaultIntervalKm = 50000,
            defaultIntervalMonths = 12,
            applicableVehicleClasses = listOf(
                VehicleClass.LIGHT_TRUCK,
                VehicleClass.MEDIUM_TRUCK,
                VehicleClass.HEAVY_TRUCK,
                VehicleClass.BUS
            ),
            guideNotes = "에어브레이크 수분 분리 필터. 동절기 에어라인 동결 방지 핵심."
        ),
        MaintenanceCatalogEntity(
            itemCode = "CHASSIS_GREASE",
            displayName = "섀시 / 조향 그리스 주입",
            category = "상용/섀시",
            priority = RulePriority.P3,
            defaultActionType = ActionType.REFILL,
            defaultIntervalKm = 10000,
            defaultIntervalMonths = 6,
            applicableVehicleClasses = listOf(
                VehicleClass.LIGHT_TRUCK,
                VehicleClass.MEDIUM_TRUCK,
                VehicleClass.HEAVY_TRUCK,
                VehicleClass.BUS
            ),
            guideNotes = "킹핀, 프로펠러 샤프트, 유니버설 조인트 그리스 니플 급유."
        )
    )
}
