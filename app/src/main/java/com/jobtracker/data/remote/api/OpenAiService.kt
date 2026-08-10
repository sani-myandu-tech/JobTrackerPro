package com.jobtracker.data.remote.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class OpenAiRequest(
    val model: String = "gpt-4o",
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3,
    val max_tokens: Int = 1500,
    val response_format: ResponseFormat? = null
)

data class ResponseFormat(val type: String = "json_object")

data class OpenAiMessage(val role: String, val content: String)

data class OpenAiResponse(
    val choices: List<Choice>
)

data class Choice(val message: OpenAiMessage)

data class AnalysisJson(
    val match_score: Int,
    val present_skills: List<String>,
    val missing_skills: List<String>,
    val suggestions: List<String>,
    val summary: String
)

interface OpenAiService {
    @POST("v1/chat/completions")
    suspend fun analyse(
        @Header("Authorization") auth: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse

    @POST("v1/chat/completions")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: OpenAiRequest
    ): OpenAiResponse
}
