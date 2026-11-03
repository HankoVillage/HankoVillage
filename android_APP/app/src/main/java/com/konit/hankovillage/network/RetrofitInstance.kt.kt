package com.konit.hankovillage.network

import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 백엔드 FastAPI 실서버와 통신하기 위한 Retrofit 싱글톤 객체
 */
object RetrofitInstance {

    private const val BASE_URL = "https://api.hankovillage.com/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    val api: HankoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(HankoApiService::class.java)
    }
}