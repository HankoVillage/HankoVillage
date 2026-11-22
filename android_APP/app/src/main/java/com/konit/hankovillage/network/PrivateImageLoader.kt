package com.konit.hankovillage.network

import android.content.Context
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object PrivateImageLoader {
    private const val BASE_URL = "https://api.hankovillage.com"

    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: buildImageLoader(context.applicationContext).also { instance = it }
        }
    }

    private fun buildImageLoader(appContext: Context): ImageLoader {
        // 기존 AuthInterceptor + ETag/304 캐시 처리용 OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .cache(Cache(File(appContext.cacheDir, "http_image_cache"), 20L * 1024 * 1024))
            .addInterceptor(AuthInterceptor(appContext))
            .build()

        return ImageLoader.Builder(appContext)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(appContext.cacheDir, "coil_image_cache"))
                    .maxSizeBytes(30L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(true) // 서버 Cache-Control / ETag(304) 존중
            .build()
    }

    /**
     * 서버가 내려준 상대 경로("/yarns/1/image")를 전체 URL로 변환
     */
    fun resolveModel(source: Any?): Any? {
        if (source is String && source.startsWith("/")) {
            return "$BASE_URL$source"
        }
        return source
    }

    /**
     * 로그아웃/회원탈퇴 시 비공개 이미지 메모리·디스크 캐시 전체 삭제
     */
    @OptIn(ExperimentalCoilApi::class)
    fun clearCache(context: Context) {
        val loader = get(context)
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }
}