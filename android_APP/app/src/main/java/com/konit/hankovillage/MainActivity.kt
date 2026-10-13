package com.konit.hankovillage

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            val signatures = info.signatures

            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())

                    val digestBytes = md.digest()

                    // 1. 카카오용 Key Hash (Base64 변환)
                    val keyHash = String(Base64.encode(digestBytes, 0)).trim()
                    Log.d("KEY_HASH", "카카오 키해시: $keyHash")

                    // 2. 구글용 SHA-1 (16진수 HEX 변환)
                    val sha1 = digestBytes.joinToString(":") { String.format("%02X", it) }
                    Log.d("KEY_HASH", "구글 SHA1: $sha1")
                }
            }
        } catch (e: Exception) {
            Log.e("KEY_HASH", "오류 발생", e)
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}