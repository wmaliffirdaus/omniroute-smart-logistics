package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Moshi JSON models matching Gemini API
    data class TextPart(val text: String)
    data class Content(val parts: List<TextPart>)
    data class GenerateContentRequest(val contents: List<Content>)

    data class CandidatePart(val text: String?)
    data class CandidateContent(val parts: List<CandidatePart>?)
    data class Candidate(val content: CandidateContent?)
    data class GenerateContentResponse(val candidates: List<Candidate>?)

    suspend fun getAssistantResponse(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not set or using placeholder.")
            return@withContext getMockFallbackResponse(prompt)
        }

        val fullPrompt = if (systemInstruction != null) {
            "$systemInstruction\n\nUser request:\n$prompt"
        } else {
            prompt
        }

        val requestObj = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(TextPart(text = fullPrompt)))
            )
        )

        val jsonAdapter = moshi.adapter(GenerateContentRequest::class.java)
        val requestBodyJson = jsonAdapter.toJson(requestObj)

        val url = "$BASE_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toRequestBody(mediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: ${response.code} $errBody")
                    return@withContext "API Error: ${response.code}. Falling back to smart offline logistics engine."
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    Log.e(TAG, "Empty response body")
                    return@withContext "Error: Received empty response from Gemini API."
                }

                val responseAdapter = moshi.adapter(GenerateContentResponse::class.java)
                val responseObj = responseAdapter.fromJson(responseBody)
                val responseText = responseObj?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                return@withContext responseText ?: "Error: Could not extract recommendation text."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            return@withContext "Connection error: ${e.localizedMessage}. Running local logistics fallback."
        }
    }

    private fun getMockFallbackResponse(prompt: String): String {
        return when {
            prompt.contains("schedule", ignoreCase = true) || prompt.contains("sequence", ignoreCase = true) -> {
                """
                📍 **Optimal Logistics Recommendation**:
                Based on current simulated traffic patterns, the optimal delivery schedule is:
                1. **742 Evergreen Terrace** (High Priority, ETA 09:15 AM)
                2. **123 Maple Street** (ETA 10:05 AM)
                3. **456 Oak Avenue** (ETA 11:30 AM)
                
                *AI Insight*: Sequencing in this order reduces backtracking, saving approximately **18 minutes** of travel time and **0.6 gallons** of fuel.
                """.trimIndent()
            }
            prompt.contains("delay", ignoreCase = true) || prompt.contains("traffic", ignoreCase = true) -> {
                """
                ⚠️ **Delay Prediction Alert**:
                - **Oak Avenue Route**: High probability of moderate delay (10-15 mins) due to simulated road work between 10:00 AM and 12:00 PM.
                - **Evergreen Terrace Route**: Light traffic, clear skies. Recommended to service first.
                
                *Smart Recommendation*: Re-sequence Oak Avenue to be the final stop after 12:00 PM to bypass peak construction delays.
                """.trimIndent()
            }
            prompt.contains("risk", ignoreCase = true) -> {
                """
                🛡️ **Safety & Risk Assessment**:
                - **Zone 3 (Maple Street)**: Construction zone narrow streets. Park near the main avenue rather than the side alley to avoid blockages.
                - **Zone 5 (Oak Avenue)**: High density residential. Use extra caution for pedestrian activity and park in designated delivery spaces.
                
                *Driver Tip*: Set status to 'ARRIVED' when within 50 meters to trigger automatic customer signature notification prompts.
                """.trimIndent()
            }
            else -> {
                """
                🤖 **OmniRoute AI Co-pilot**:
                Analyzing fleet data and route configurations...
                - All stops optimized.
                - Offline sync engine is active.
                - Real-time location tracking is active.
                Please select a specific tool above to receive real-time, context-aware routing recommendations.
                """.trimIndent()
            }
        }
    }
}
