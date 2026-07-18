package com.example.api

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class EmbedRequest(
    val model: String = "models/text-embedding-004",
    val content: EmbedContent
)

@JsonClass(generateAdapter = true)
data class EmbedContent(
    val parts: List<EmbedPart>
)

@JsonClass(generateAdapter = true)
data class EmbedPart(
    val text: String
)

@JsonClass(generateAdapter = true)
data class EmbedResponse(
    val embedding: EmbeddingData
)

@JsonClass(generateAdapter = true)
data class EmbeddingData(
    val values: List<Float>
)

interface GeminiEmbeddingService {
    @POST("v1beta/models/text-embedding-004:embedContent")
    suspend fun embedContent(
        @Query("key") apiKey: String,
        @Body request: EmbedRequest
    ): EmbedResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        })
        .build()

    val service: GeminiEmbeddingService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiEmbeddingService::class.java)
    }
}
