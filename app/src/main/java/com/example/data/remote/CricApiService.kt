package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CricApiSeriesResponse(
    @Json(name = "apikey") val apiKey: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "data") val data: List<CricApiSeriesDto>? = null
)

@JsonClass(generateAdapter = true)
data class CricApiSeriesDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "startDate") val startDate: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "odi") val odi: Int? = 0,
    @Json(name = "t20") val t20: Int? = 0,
    @Json(name = "test") val test: Int? = 0,
    @Json(name = "squads") val squads: Int? = 0,
    @Json(name = "matches") val matches: Int? = 0
)

@JsonClass(generateAdapter = true)
data class CricApiSeriesInfoResponse(
    @Json(name = "apikey") val apiKey: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "data") val data: CricApiSeriesInfoDataDto? = null
)

@JsonClass(generateAdapter = true)
data class CricApiSeriesInfoDataDto(
    @Json(name = "info") val info: CricApiSeriesInfoMetaDto? = null,
    @Json(name = "matchList") val matchList: List<CricApiMatchDto>? = null
)

@JsonClass(generateAdapter = true)
data class CricApiSeriesInfoMetaDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "startdate") val startDate: String? = null,
    @Json(name = "enddate") val endDate: String? = null,
    @Json(name = "odi") val odi: Int? = 0,
    @Json(name = "t20") val t20: Int? = 0,
    @Json(name = "test") val test: Int? = 0,
    @Json(name = "matches") val matches: Int? = 0
)

@JsonClass(generateAdapter = true)
data class CricApiMatchesResponse(
    @Json(name = "apikey") val apiKey: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "data") val data: List<CricApiMatchDto>? = null
)

@JsonClass(generateAdapter = true)
data class CricApiMatchDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "matchType") val matchType: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "venue") val venue: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "dateTimeGMT") val dateTimeGmt: String? = null,
    @Json(name = "teams") val teams: List<String>? = null,
    @Json(name = "score") val score: List<CricApiScoreDto>? = null,
    @Json(name = "series_id") val seriesId: String? = null,
    @Json(name = "matchStarted") val matchStarted: Boolean? = null,
    @Json(name = "matchEnded") val matchEnded: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CricApiScoreDto(
    @Json(name = "r") val runs: Int? = null,
    @Json(name = "w") val wickets: Int? = null,
    @Json(name = "o") val overs: Double? = null,
    @Json(name = "inning") val inning: String? = null
)

interface CricApiService {
    @GET("series")
    suspend fun getSeries(
        @Query("apikey") apiKey: String,
        @Query("offset") offset: Int = 0
    ): CricApiSeriesResponse

    @GET("series_info")
    suspend fun getSeriesInfo(
        @Query("apikey") apiKey: String,
        @Query("id") seriesId: String
    ): CricApiSeriesInfoResponse

    @GET("matches")
    suspend fun getMatches(
        @Query("apikey") apiKey: String,
        @Query("offset") offset: Int = 0
    ): CricApiMatchesResponse

    @GET("currentMatches")
    suspend fun getCurrentMatches(
        @Query("apikey") apiKey: String,
        @Query("offset") offset: Int = 0
    ): CricApiMatchesResponse
}

object CricApiClientFactory {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun create(baseUrl: String): CricApiService {
        val safeBaseUrl = try {
            val trimmed = baseUrl.trim()
            val normalized = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
                normalized
            } else {
                "https://api.cricapi.com/v1/"
            }
        } catch (_: Exception) {
            "https://api.cricapi.com/v1/"
        }

        return Retrofit.Builder()
            .baseUrl(safeBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CricApiService::class.java)
    }
}
