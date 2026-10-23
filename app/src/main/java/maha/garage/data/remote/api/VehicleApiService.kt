package maha.garage.data.remote.api

import maha.garage.data.remote.dto.PublicCarApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface VehicleApiService {

    /**
     * 공공데이터포털 국토교통부 자동차관리정보시스템(TS) 자동차 기본제원 조회
     * @param plateNumber 차량등록번호 (예: 12가3456)
     */
    @GET("1613000/CarInformationService/getCarInfoDetail")
    suspend fun getVehicleSpec(
        @Query("vhrno") plateNumber: String,
        @Query("_type") type: String = "json",
        @Query("numOfRows") numOfRows: Int = 1,
        @Query("pageNo") pageNo: Int = 1
    ): PublicCarApiResponse
}
