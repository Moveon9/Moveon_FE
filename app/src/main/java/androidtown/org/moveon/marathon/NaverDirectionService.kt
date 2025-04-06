package androidtown.org.moveon.marathon

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface NaverDirectionService {
    @GET("map-direction/v1/driving")
    suspend fun  getDirection(
        @Header("X-NCP-APIGW-API-KEY-ID") clientId: String,
        @Header("X-NCP-APIGW-API-KEY") clientSecret: String,
        @Query("start") start: String,      // 예: "126.978371,37.566609"
        @Query("goal") goal: String,
        @Query("option") option: String = "traoptimal"
    ): Response<DirectionResponse>
}