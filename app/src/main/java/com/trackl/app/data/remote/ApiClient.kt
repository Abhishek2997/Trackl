package com.trackl.app.data.remote

import java.io.File
import java.io.IOException
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Must extend IOException so OkHttp reports it as a normal call failure. */
class MissingApiKeyException : IOException("TMDb API key is missing")

object ApiClient {
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun create(apiKey: String, cacheDir: File, debug: Boolean): TmdbApi {
        val auth = Interceptor { chain ->
            if (apiKey.isBlank()) throw MissingApiKeyException()
            val request = chain.request()
            // v4 "API Read Access Token" is a JWT (starts with eyJ); otherwise treat it as a v3 API key.
            val authed = if (apiKey.startsWith("eyJ")) {
                request.newBuilder().header("Authorization", "Bearer $apiKey").build()
            } else {
                request.newBuilder()
                    .url(request.url.newBuilder().addQueryParameter("api_key", apiKey).build())
                    .build()
            }
            chain.proceed(authed)
        }
        val client = OkHttpClient.Builder()
            .cache(Cache(File(cacheDir, "http"), 20L * 1024 * 1024))
            .addInterceptor(auth)
            .apply {
                if (debug) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    fun image(path: String?, size: String = "w342"): String? = path?.let { "$IMAGE_BASE$size$it" }
}

fun Throwable.userMessage(): String = when (this) {
    is MissingApiKeyException -> "TMDb API key is missing. Add TMDB_API_KEY to local.properties and rebuild."
    is HttpException -> when (code()) {
        401 -> "TMDb rejected the API key (401). Check TMDB_API_KEY."
        404 -> "Not found on TMDb."
        429 -> "Too many requests. Try again in a moment."
        else -> "TMDb error ${code()}."
    }
    is IOException -> "Network error. Check your connection."
    else -> message ?: "Something went wrong."
}
