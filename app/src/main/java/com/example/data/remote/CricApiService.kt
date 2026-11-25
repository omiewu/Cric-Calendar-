package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
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
