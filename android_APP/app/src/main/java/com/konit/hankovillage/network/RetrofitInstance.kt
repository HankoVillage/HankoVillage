package com.konit.hankovillage.network

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 백엔드 FastAPI 실서버(https://api.hankovillage.com/) 통신용 Retrofit 싱글톤 객체
 */
object RetrofitInstance {
    private const val BASE_URL = "https://api.hankovillage.com/"
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    private fun getOkHttpClient(context: Context): OkHttpClient {
        return okHttpClient ?: OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context.applicationContext))
            .build()
            .also { okHttpClient = it }
    }

    /**
     * Context를 받아 AuthInterceptor가 부착된 HankoApiService를 반환합니다.
     */
    fun getApiService(context: Context): HankoApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HankoApiService::class.java)
    }
}