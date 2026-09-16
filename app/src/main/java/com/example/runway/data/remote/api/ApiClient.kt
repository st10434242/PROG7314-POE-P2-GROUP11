package com.example.runway.data.remote.api

import com.example.runway.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

// Builds the single Retrofit client used by the app (IIE, 2026; Square, Inc., n.d.).
// The release build talks to the API over HTTPS (Google, 2020).

object ApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                // Request and response headers in Logcat while developing.
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        // HEADERS, not BODY: a try-on request carries photos as
                        // base64, and writing megabytes of it to Logcat stalls the
                        // app for minutes and truncates the log.
                        level = HttpLoggingInterceptor.Level.HEADERS
                        redactHeader("Authorization")
                    }
                )
            }
        }
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val api: RunwayApi = retrofit(okHttpClient)

    // A virtual try-on runs a diffusion model: twenty seconds for one garment and
    // roughly twice that for two. The ordinary sixty-second read timeout would cut
    // it off, so that one call gets a client of its own rather than making every
    // other request wait three minutes before giving up.
    private val renderHttpClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(RENDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val renderApi: RunwayApi = retrofit(renderHttpClient)

    private fun retrofit(client: OkHttpClient): RunwayApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(CONTENT_TYPE.toMediaType()))
        .build()
        .create(RunwayApi::class.java)

    private const val CONTENT_TYPE = "application/json"
    private const val CONNECT_TIMEOUT_SECONDS = 20L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val RENDER_TIMEOUT_SECONDS = 240L
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
Google, 2020. Secure your site with HTTPS. [online] Available at: <https://support.google.com/webmasters/answer/6073543?hl=en> [Accessed 31 July 2023].
Square, Inc., n.d.. Retrofit: A type-safe HTTP client for Android and Java. [online] Available at: <https://square.github.io/retrofit/> [Accessed 31 July 2023].
*/
