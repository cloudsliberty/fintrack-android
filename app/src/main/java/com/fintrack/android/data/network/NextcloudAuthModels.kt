package com.fintrack.android.data.network

/**
 * Nextcloud Login Flow v2 (https://docs.nextcloud.com/server/latest/developer_manual/client_apis/LoginFlow/index.html#login-flow-v2).
 * This is how any third-party client — this app included — obtains an
 * app password without ever seeing the person's real Nextcloud password:
 *  1. POST {server}/index.php/login/v2 -> LoginFlowInit
 *  2. Open `login` in the system browser; the person signs in there and
 *     approves "FinTrack" (or whatever the browser shows as the client name)
 *  3. Poll `poll.endpoint` with `poll.token` until the server responds with
 *     the granted LoginFlowCredentials (or the 15-minute flow expires)
 */
data class LoginFlowInit(
    val poll: LoginFlowPoll,
    val login: String
)

data class LoginFlowPoll(
    val token: String,
    val endpoint: String
)

data class LoginFlowCredentials(
    val server: String,
    val loginName: String,
    val appPassword: String
)
