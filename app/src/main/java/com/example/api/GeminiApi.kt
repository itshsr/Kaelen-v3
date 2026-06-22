package com.example.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Request data structures
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
    val systemInstruction: Content? = null
)

data class Content(
    val parts: List<Part>,
    val role: String? = null
)

data class Blob(
    val mimeType: String,
    val data: String
)

data class Part(
    val text: String? = null,
    val inlineData: Blob? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
)

// Tool / function-calling data structures
data class Tool(
    val googleSearch: Map<String, Any>? = null,
    val functionDeclarations: List<FunctionDeclaration>? = null
)

data class FunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

data class FunctionParameters(
    val type: String = "OBJECT",
    val properties: Map<String, PropertySchema>,
    val required: List<String> = emptyList()
)

data class PropertySchema(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null
)

data class FunctionCall(
    val name: String,
    val args: Map<String, Any?>? = null
)

data class FunctionResponse(
    val name: String,
    val response: Map<String, Any?>
)

// Response data structures
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}
