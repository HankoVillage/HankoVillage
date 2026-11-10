package com.konit.hankovillage.network

import android.content.Context
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TokenAuthenticator(private val context: Context) : Authenticator {

    private val mutex = Mutex()

    // 재발급 전용 클라이언트 (AuthInterceptor/Authenticator 제외하여 무한 루프 방지)
    private val refreshApi: HankoApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.hankovillage.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HankoApiService::class.java)
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // 재시도 횟수 제한 (무한 루프 방지)
        if (responseCount(response) >= 2) {
            return null
        }

        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        return runBlocking {
            mutex.withLock {
                val currentAccessToken = SessionManager.getAccessToken(context)

                // 다른 요청이 이미 토큰을 새로 발급받았다면 새 토큰으로 바로 재시도
                if (!currentAccessToken.isNullOrEmpty() && currentAccessToken != requestToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .build()
                }

                val refreshToken = SessionManager.getRefreshToken(context)
                if (refreshToken.isNullOrEmpty()) {
                    SessionManager.notifySessionExpired(context)
                    return@withLock null
                }

                try {
                    val refreshResponse = refreshApi.refreshTokens(RefreshRequest(refreshToken))

                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val body = refreshResponse.body()!!
                        SessionManager.saveTokens(context, body.accessToken, body.refreshToken)

                        return@withLock response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.accessToken}")
                            .build()
                    } else {
                        // 401 INVALID_REFRESH_TOKEN 등 재발급 실패 시 토큰 삭제 후 로그인 화면 이동
                        if (refreshResponse.code() == 401) {
                            SessionManager.notifySessionExpired(context)
                        }
                        return@withLock null
                    }
                } catch (e: Exception) {
                    return@withLock null
                }
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}