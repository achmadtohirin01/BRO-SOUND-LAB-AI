package com.example.data.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- DATA SCHEMAS FOR GEMINI REST CALLS ---

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

// --- PARSED STRUCTURED MASTERING OUTPUTS ---

@JsonClass(generateAdapter = true)
data class AiMasteringPreset(
    val b_31: Float = 0f,
    val b_62: Float = 0f,
    val b_125: Float = 0f,
    val b_250: Float = 0f,
    val b_500: Float = 0f,
    val b_1k: Float = 0f,
    val b_2k: Float = 0f,
    val b_4k: Float = 0f,
    val b_8k: Float = 0f,
    val b_16k: Float = 0f,
    val compressor_threshold: Float = -15f,
    val reverb_decay: Float = 0.2f,
    val delay_feedback: Float = 0.2f,
    val drive: Float = 0f,
    val wideness: Float = 0.5f,
    val comment: String = "Engineered preset."
)

@JsonClass(generateAdapter = true)
data class AiLyricsSheet(
    val title: String = "Untitled Track",
    val bpm: Int = 120,
    val key: String = "A Minor",
    val vibe_notes: String = "",
    val lyrics: String = ""
)

// --- RETROFIT SERVICE INTERFACE ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- RETROFIT SERVICE CLIENT ---

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Injects 60-second timeouts to allow complex AI processing to output complete structures
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    // Moshi string Parsers
    fun parseMasteringPreset(json: String): AiMasteringPreset? {
        return try {
            val stripped = cleanJsonBlock(json)
            moshi.adapter(AiMasteringPreset::class.java).fromJson(stripped)
        } catch (e: Exception) {
            null
        }
    }

    fun parseLyricsSheet(json: String): AiLyricsSheet? {
        return try {
            val stripped = cleanJsonBlock(json)
            moshi.adapter(AiLyricsSheet::class.java).fromJson(stripped)
        } catch (e: Exception) {
            null
        }
    }

    // Helper: Strip markdown ```json codes blocks
    private fun cleanJsonBlock(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substringAfter("```json").substringBeforeLast("```")
        } else if (str.startsWith("```")) {
            str = str.substringAfter("```").substringBeforeLast("```")
        }
        return str.trim()
    }
}
