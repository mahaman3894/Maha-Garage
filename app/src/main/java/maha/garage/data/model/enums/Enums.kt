package maha.garage.data.model.enums

enum class PowertrainType(val displayName: String) {
    GASOLINE("가솔린(휘발유)"),
    DIESEL("디젤(경유)"),
    LPG("LPG"),
    HEV_GASOLINE("가솔린 하이브리드"),
    HEV_DIESEL("디젤 하이브리드"),
    PHEV_GASOLINE("플러그인 하이브리드"),
    PHEV_DIESEL("디젤 PHEV"),
    BEV("순수 전기차(EV)"),
    OTHER("기타")
}

enum class VehicleClass(val displayName: String) {
    SEDAN("승용 세단"),
    SUV("SUV / CUV"),
    VAN("미니밴 / 승합"),
    PICKUP("픽업 트럭"),
    LIGHT_TRUCK("1톤 / 소형 화물"),
    MEDIUM_TRUCK("중형 화물"),
    HEAVY_TRUCK("대형 화물"),
    BUS("대형 버스"),
    OTHER("특장 / 기타")
}

enum class TireMode(val displayName: String) {
    SUMMER("섬머 / 사계절 타이어"),
    WINTER("윈터(스노우) 타이어")
}

enum class ActionType(val displayName: String) {
    INSPECT("점검"),
    REPLACE("교체"),
    REFILL("보충"),
    ROTATE("위치교환"),
    SWAP("스왑"),
    CLEAN("청소/세척")
}

enum class RulePriority(val level: String, val displayName: String) {
    P1("P1", "최우선 정기 소모품"),
    P2("P2", "주행 체감 및 공조"),
    P3("P3", "구동 및 점화 계통"),
    P4("P4", "주기적 교체/내구 부품"),
    P5("P5", "점검 및 섀시 세이프티")
}

enum class SourceType(val displayName: String) {
    USER_OVERRIDE("사용자 직접 설정"),
    VERIFIED_OEM("제조사 공시 정비표"),
    OEM_FAMILY("동일 파워트레인 기준"),
    GENERIC_ESTIMATE("일반 표준 추천치"),
    NOT_CONFIGURED("제조사 기준 미확인")
}

enum class DueStatus(val displayName: String) {
    OVERDUE("교체 필요"),
    DUE_NOW("교체 필요"),
    DUE_SOON("교체 예정"),
    INSPECTION_DUE("점검 필요"),
    NORMAL("정상"),
    NEEDS_DATA("기록 없음")
}
