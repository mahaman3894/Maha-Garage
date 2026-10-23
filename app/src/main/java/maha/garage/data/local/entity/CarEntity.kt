package maha.garage.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass
import java.util.UUID

@Entity(tableName = "cars")
data class CarEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val carNumber: String,              // 예: "12가 3456"
    val modelName: String,              // 예: "335i Convertible", "Pilot"
    val nickname: String? = null,       // 예: "내 애마", "패밀리카"
    val make: String = "",              // 예: "BMW", "Honda", "현대"
    val firstRegistrationDate: String? = null, // yyyy-MM-dd: 최초 등록일, 모르면 null
    val modelYear: Int = 2020,          // 예: 2012, 2024
    val powertrain: PowertrainType = PowertrainType.GASOLINE,
    val vehicleClass: VehicleClass = VehicleClass.SEDAN,
    val currentMileage: Int = 0,        // 현재 누적 주행거리 (km)
    val engineHours: Double? = null,    // 상용/트럭용 누적 엔진 가동시간 (시간)
    
    // 섬머 / 윈터 타이어 스왑 추적 기능
    val currentTireMode: String = "SUMMER", // "SUMMER" or "WINTER"
    val summerTireMileage: Int = 0,         // 섬머 타이어 상태 누적 주행거리
    val winterTireMileage: Int = 0,         // 윈터 타이어 상태 누적 주행거리
    val lastSwapMileage: Int = 0,           // 마지막 타이어 스왑 시점의 차량 주행거리
    val summerTireName: String? = null,     // 예: "미쉐린 PS4 245/40R18"
    val winterTireName: String? = null,     // 예: "한국타이어 윈터아이셉트 에보3"
    
    val photoUri: String? = null,           // 차량 대표 사진
    val isSevereCondition: Boolean = false, // 가혹 운행조건 여부 (하위호환 유지)
    val maintenanceStandard: String = "OFFICIAL", // "OFFICIAL" (공식 표준), "SEVERE" (공식 가혹), "SHOP" (정비소/오너 권장)
    val wheelAlignmentMileage: Int? = null, // 최근 휠 얼라인먼트 정비 주행거리 (km)
    val isArchived: Boolean = false,        // 소프트 삭제 여부
    val updatedAt: Long = System.currentTimeMillis(),

    // 제원 및 타이밍 구동계 필드
    val timingType: String = "CHAIN",       // "CHAIN", "BELT", "NA", "UNKNOWN"
    val timingDesc: String? = null,         // 타이밍 구동 설명 및 권장안내
    val trimName: String? = null,           // 세부 라인업/트림명
    val displacement: String? = null,       // 배기량 (예: "1,598 cc")
    val power: String? = null,              // 최고출력 (예: "180 ps")
    val torque: String? = null,             // 최대토크 (예: "27.0 kg.m")
    val transmission: String? = null,       // 변속기
    val driveSystem: String? = null,        // 구동방식
    val fuelEconomy: String? = null,        // 공인연비
    val dimensions: String? = null,         // 전장x전폭x전고 (축거)

    // 정기/종합 검사 및 법정 규정 관리 필드 (미입력 시 null, 공식 확인 필요 정책 적용)
    val isCommercial: Boolean = false,               // 사업용 여부 (true: 사업용, false: 비사업용/자가용)
    val inspectionType: String = "정기검사",           // "정기검사", "종합검사", "확인 필요"
    val inspectionStartDate: String? = null,         // 검사 가능 시작일 (모르면 null)
    val inspectionEndDate: String? = null,           // 검사 유효기간 만료일 (모르면 null)
    val isInspectionDateEstimated: Boolean = true,   // 예상 일정(공식 확인 필요) 여부
    val lastInspectionDate: String? = null,          // 최근 검사일
    val lastInspectionResult: String? = null,        // 최근 검사 결과 (예: "적합", "재검사 필요")
    val retestDeadline: String? = null,              // 부적합 판정 시 법정 재검사 기한 (모르면 null)

    // 자동차 보험 가입 정보 필드 (미입력 시 null)
    val insuranceCompany: String? = null,            // 보험사
    val insuranceStartDate: String? = null,          // 보험 가입일
    val insuranceEndDate: String? = null,            // 보험 만료일 (가입일 + 1년)
    val insurancePolicyNumber: String? = null,       // 증권번호
    val insuranceEmergencyCall: String? = null       // 긴급출동 전화번호
)

