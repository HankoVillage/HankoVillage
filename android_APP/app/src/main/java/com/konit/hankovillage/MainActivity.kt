package com.konit.hankovillage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.kakao.sdk.common.KakaoSdk

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}