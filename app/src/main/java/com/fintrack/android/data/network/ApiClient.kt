package com.fintrack.android.data.network

import android.content.Context
import android.util.Base64
import com.fintrack.android.BuildConfig
import com.fintrack.android.data.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var cachedApi: FinTrackApi? = null
    private var cachedForServer: String? = null

    // Release builds stay silent — BASIC level only logs method/URL/status/timing (never the
    // Authorization header or body), but there's no reason for a shipped build to write any of
    // that to Logcat at all.
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
    }

    /**
     * Adds the Basic Auth header (Nextcloud login name + app password) to
     * every request. Nextcloud accepts Basic Auth on regular (non-OCS)
     * controller routes exactly like it does for WebDAV/OCS clients — no
     * separate CSRF token is needed for non-session requests, which is what
     * makes a plain REST client viable here at all.
     */
    private fun authInterceptor(context: Context): Interceptor = Interceptor { chain ->
        val session = SessionManager.current(context)
        val request = if (session != null) {
            val credentials = Base64.encodeToString(
                "${session.loginName}:${session.appPassword}".toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            chain.request().newBuilder()
                .header("Authorization", "Basic $credentials")
                .header("OCS-APIREQUEST", "true")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /** Returns a ready-to-use FinTrack API client for the current session, or null if not logged in. */
    fun finTrack(context: Context): FinTrackApi? {
        val baseUrl = SessionManager.apiBaseUrl(context) ?: return null
        if (cachedApi != null && cachedForServer == baseUrl) return cachedApi

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor(context))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(FinTrackApi::class.java).also {
            cachedApi = it
            cachedForServer = baseUrl
        }
    }

    /** Call after logout so a subsequent login for a different account/server builds a fresh client. */
    fun invalidate() {
        cachedApi = null
        cachedForServer = null
    }

    /**
     * Unauthenticated client used only for the Login Flow v2 handshake,
     * against whatever server URL the person typed on the login screen
     * (each call supplies its own full @Url, so no fixed base URL is needed
     * — Retrofit still requires a placeholder base URL to be configured).
     */
    fun nextcloudAuth(): NextcloudAuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://localhost/") // unused placeholder — every call passes a full @Url
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(NextcloudAuthApi::class.java)
    }
}
