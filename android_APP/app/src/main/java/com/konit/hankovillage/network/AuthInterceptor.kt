package com.konit.hankovillage.network

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response

/**
 * SharedPreferences에 저장된 access_token을 읽어와
 * 모든 HTTP 요청의 Authorization: Bearer 헤더에 자동으로 실어주는 Interceptor
 */
class AuthInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        // 로컬 저장소에서 access_token 읽기
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val accessToken = sharedPref.getString("access_token", null)

        val requestBuilder = chain.request().newBuilder()

        // 토큰이 존재할 경우 Authorization: Bearer <access_token> 헤더 부착
        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}