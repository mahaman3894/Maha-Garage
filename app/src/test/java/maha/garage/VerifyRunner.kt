package maha.garage

fun main() {
    println("==================================================")
    println("   마하개러지 종합 검증 & Quality Gate 자동 테스트  ")
    println("==================================================")

    // 1. ApplicabilityResolver 검증
    println("\n[1/4] ApplicabilityResolver (소모품 적용성 엔진) 테스트:")
    val resolverTest = ApplicabilityResolverTest()
    resolverTest.`전기차(BEV)에는 엔진오일, 점화플러그가 배제되고 감속기오일이 포함되어야 한다`()
    println("  [PASS] 전기차(BEV) 소모품 배제/포함 검증 성공")
    resolverTest.`디젤 차량에는 점화플러그가 배제되고 연료필터 및 수분분리기가 포함되어야 한다`()
    println("  [PASS] 디젤 전용 소모품(연료필터, 수분분리기) 검증 성공")
    resolverTest.`상용 트럭에는 에어드라이어 및 섀시 그리스 주입이 오버레이 적용되어야 한다`()
    println("  [PASS] 상용차(포터/봉고/대형) 오버레이 검증 성공")
    resolverTest.`가혹 운행조건 선택 시 가혹 주기값으로 우선 설정되어야 한다`()
    println("  [PASS] 가혹조건 주기 우선 반영 검증 성공")

    // 2. MaintenanceScheduleEngine 검증
    println("\n[2/4] MaintenanceScheduleEngine (정비 주기 및 도래 판정 엔진) 테스트:")
    val engineTest = MaintenanceScheduleEngineTest()
    engineTest.`주행거리 기준 - 교체 주기 여유 시 NORMAL 상태여야 한다`()
    println("  [PASS] 교체 주기 여유 NORMAL 상태 판정 성공")
    engineTest.`주행거리 기준 - 잔여 1000km 이내 도래 시 DUE_SOON 상태여야 한다`()
    println("  [PASS] 1000km 이내 임박 DUE_SOON 상태 판정 성공")
    engineTest.`주행거리 기준 - 주기 초과 시 OVERDUE 상태여야 한다`()
    println("  [PASS] 주행거리 초과 OVERDUE 상태 판정 성공")
    engineTest.`날짜 기준 - 12개월 초과 경과 시 거리가 남아있어도 OVERDUE 상태여야 한다`()
    println("  [PASS] 날짜 만료 OVERDUE 상태 판정 성공")
    engineTest.`상태 기반 부품 - 잔여 3mm 이하 측정 시 즉시 교체 OVERDUE 상태여야 한다`()
    println("  [PASS] 상태 기반 패드 마모 한계선 OVERDUE 상태 판정 성공")

    // 3. TireSwapCalculator 검증
    println("\n[3/4] TireSwapCalculator (타이어 스왑 및 거리 계산기) 테스트:")
    val tireTest = TireSwapCalculatorTest()
    tireTest.`실시간 타이어 거리 계산 - 현재 장착 모드 타이어에만 미기록 주행거리가 합산되어야 한다`()
    println("  [PASS] 실시간 타이어 주행거리 합산 계산 성공")
    tireTest.`타이어 스왑 수행 - 섬머에서 윈터로 스왑 시 이전 주행거리가 누적되고 신규 모드로 정상 전환되어야 한다`()
    println("  [PASS] 섬머->윈터 타이어 스왑 및 로그 생성 성공")

    // 4. Quality Gate 6대 출시 점검 테스트
    println("\n[4/4] 5단계 Quality Gate 점검 리스트 검증:")
    val qg = QualityGateTest()
    qg.test1_BEV_Exclusion_And_Inclusion()
    println("  [PASS] Q1: 전기차(BEV) 내연기관 완전 배제 및 감속기오일/인버터냉각수 검증")
    qg.test2_Commercial_Diesel_Truck_Overlay()
    println("  [PASS] Q2: 디젤 상용 트럭 에어드라이어/섀시그리스 오버레이 검증")
    qg.test3_HEV_Hybrid_Components()
    println("  [PASS] Q3: HEV 하이브리드 HSG벨트 및 냉각수 조건부 공존 검증")
    qg.test4_Inspect_vs_Replace_Semantics()
    println("  [PASS] Q4: INSPECT(측정치보존) vs REPLACE(주기리셋) 정밀 분기 검증")
    qg.test5_MultiLine_Batch_Evaluation()
    println("  [PASS] Q5: 1회 방문 다중 소모품 동시 정비 및 개별 갱신 검증")
    qg.test6_JSON_Backup_RoundTrip_Data_Integrity()
    println("  [PASS] Q6: JSON 백업/복원 무손실 라운드트립 무결성 검증")
    qg.test7_FirstInterval_EV_Coolant()
    println("  [PASS] Q7: EV 냉각수 등 최초/반복 교체주기 분리 계산 검증")
    qg.test8_MaxOf_Progress_Progression()
    println("  [PASS] Q8: 저주행 차량 기간 만기 maxOf 소모 진행률 평가 검증")

    println("\n==================================================")
    println("  축하합니다! 모든 핵심 도메인 및 Quality Gate 통과 (19/19 SUCCESS) ")
    println("==================================================")
}
