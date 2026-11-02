package com.konit.hankovillage.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 백엔드 FastAPI 서버와 통신하기 위한 Retrofit 싱글톤 객체
 */
object RetrofitInstance {
    // 안드로이드 에뮬레이터에서 로컬 PC 백엔드(localhost:8000)를 바라보는 전용 IP
    private const val BASE_URL = "https://api.hankovillage.com"

    val api: HankoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HankoApiService::class.java)
    }
}