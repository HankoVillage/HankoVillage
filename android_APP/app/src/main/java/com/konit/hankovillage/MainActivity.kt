package com.konit.hankovillage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // 📱 5개 탭 바가 뚫린 메인 뼈대 화면을 띄웁니다!
                MainScreen()
            }
        }
    }
}