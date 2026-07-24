package com.fintrack.android.data.network

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

interface NextcloudAuthApi {

    /** Kicks off Login Flow v2 against the user-supplied server URL. */
    @POST
    suspend fun initLoginFlow(@Url url: String): LoginFlowInit

    /**
     * Polled every ~1.5s while the login page is open in the browser. Nextcloud
     * returns 404 until the person finishes signing in and approves access —
     * that's expected and just means "keep polling", not a real error.
     */
    @FormUrlEncoded
    @POST
    suspend fun pollLoginFlow(@Url url: String, @Field("token") token: String): Response<LoginFlowCredentials>
}
