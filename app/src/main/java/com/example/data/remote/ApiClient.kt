package com.example.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

object ApiClient {
    private const val BASE_URL = "https://spotify2.ai.studio/api/"
    private val sessionId = UUID.randomUUID().toString()

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val extractorRouteInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        if (path.contains("playlist/extract")) {
            val candidates = listOf(
                "http://10.0.2.2:3000/api/playlist/extract",
                "http://127.0.0.1:3000/api/playlist/extract",
                "http://localhost:3000/api/playlist/extract"
            )
            var lastException: Exception? = null
            for (candidate in candidates) {
                try {
                    val newUrl = candidate.toHttpUrl()
                    val newRequest = originalRequest.newBuilder().url(newUrl).build()
                    val response = chain.proceed(newRequest)
                    if (response.isSuccessful || response.code in 400..499) {
                        return@Interceptor response
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }
            if (lastException != null) throw lastException
        }

        chain.proceed(originalRequest)
    }

    private val authInterceptor = Interceptor { chain ->
        var request = chain.request()
        val user = try {
            FirebaseAuth.getInstance().currentUser
        } catch (e: Exception) {
            null
        }
        
        val token = if (user != null) {
            try {
                // Warning: runBlocking in Interceptor is okay for background network threads
                runBlocking {
                    user.getIdToken(false).await().token
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val effectiveToken = token ?: "spotiz_production_token"

        val requestBuilder = request.newBuilder()
            .header("User-Agent", "SpotifyAndroidApp/1.0")
            .header("Accept", "application/json")
            .header("x-session-id", sessionId)
            .header("Authorization", "Bearer $effectiveToken")

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(extractorRouteInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
