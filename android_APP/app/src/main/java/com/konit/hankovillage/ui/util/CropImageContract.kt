package com.konit.hankovillage.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

/**
 * 이미지 선택 계약 (크래시 유발 외부 CROP 호출 제거)
 */
class CropImageContract : ActivityResultContract<Uri, Uri?>() {
    private var inputUri: Uri? = null

    override fun createIntent(context: Context, input: Uri): Intent {
        inputUri = input
        return Intent()
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return inputUri
    }
}