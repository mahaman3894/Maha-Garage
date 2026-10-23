package maha.garage.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import maha.garage.data.model.enums.PowertrainType
import maha.garage.data.model.enums.VehicleClass

/**
 * 공공데이터포털(data.go.kr) 국토교통부 자동차관리정보시스템(TS) 응답 규격
 */
@JsonClass(generateAdapter = true)
data class PublicCarApiResponse(
    @Json(name = "response") val response: CarResponseContainer? = null
)

@JsonClass(generateAdapter = true)
data class CarResponseContainer(
    @Json(name = "header") val header: CarResponseHeader? = null,
    @Json(name = "body") val body: CarResponseBody? = null
)

@JsonClass(generateAdapter = true)
data class CarResponseHeader(
    @Json(name = "resultCode") val resultCode: String? = null,
    @Json(name = "resultMsg") val resultMsg: String? = null
)

@JsonClass(generateAdapter = true)
data class CarResponseBody(
    @Json(name = "items") val items: CarItemsContainer? = null,
    @Json(name = "totalCount") val totalCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class CarItemsContainer(
    @Json(name = "item") val itemList: List<CarSpecItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class CarSpecItemDto(
    @Json(name = "vhrno") val plateNumber: String? = null,       // 자동차 등록번호 (예: 12가3456)
    @Json(name = "vhctp") val carModelName: String? = null,      // 자동차 차명/모델 (예: 캠리 하이브리드)
    @Json(name = "makrNm") val manufacturer: String? = null,     // 제작사 (예: 현대, 기아, 토요타)
    @Json(name = "pyvl") val displacement: String? = null,       // 배기량 (cc)
    @Json(name = "fuelNm") val fuelType: String? = null,         // 연료 (휘발유, 경유, 하이브리드, 전기 등)
    @Json(name = "crtype") val carCategory: String? = null,      // 차종 (승용, 화물, 특수 등)
    @Json(name = "frstRegistDt") val regDate: String? = null,    // 최초 등록일자 (YYYYMMDD)
    @Json(name = "vldBgngDt") val inspectStartDate: String? = null, // 검사 유효 시작일
    @Json(name = "vldEndDt") val nextInspectDate: String? = null    // 검사 유효 만료일 (정기검사일)
)

/**
 * 앱 내부에서 사용하기 편하도록 가공된 표준 차량 제원 DTO
 */
data class VehicleSpecDto(
    val plateNumber: String,
    val modelName: String?,
    val manufacturer: String?,
    val fuelType: String?,
    val displacement: String?,
    val registrationDate: String?,
    val nextInspectionDate: String?,
    val carCategory: String?,
    val powertrain: PowertrainType = PowertrainType.GASOLINE,
    val vehicleClass: VehicleClass = VehicleClass.SEDAN,
    val modelYear: Int = 2023
)
