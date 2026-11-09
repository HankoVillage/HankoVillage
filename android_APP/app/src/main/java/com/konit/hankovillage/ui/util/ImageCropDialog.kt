package com.konit.hankovillage.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropComplete: (Uri) -> Unit
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        try {
            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            null
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) {
                    Text("취소", color = Color.White)
                }
                TextButton(
                    onClick = {
                        if (bitmap != null && boxSize.width > 0) {
                            val croppedUri = cropBitmapTo1By1(
                                context = context,
                                bitmap = bitmap,
                                boxSize = boxSize.width.toFloat(),
                                scale = scale,
                                offset = offset
                            )
                            if (croppedUri != null) {
                                onCropComplete(croppedUri)
                            }
                        }
                    }
                ) {
                    Text("완료", color = Color(0xFFD36D33))
                }
            }

            // 1:1 정사각형 크롭 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clipToBounds()
                    .background(Color.DarkGray)
                    .onSizeChanged { boxSize = it }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = max(1f, (scale * zoom).coerceAtMost(4f))
                            offset += pan
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "크롭할 이미지",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }

                // 1:1 가이드라인 테두리
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.8f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Text(
                text = "사진을 드래그하거나 두 손가락으로 확대해 원하는 부분을 맞추세요.",
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

private fun cropBitmapTo1By1(
    context: Context,
    bitmap: Bitmap,
    boxSize: Float,
    scale: Float,
    offset: Offset
): Uri? {
    return try {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()

        // ContentScale.Crop 기준 초기 배율 계산
        val baseScale = max(boxSize / bw, boxSize / bh)
        val totalScale = baseScale * scale

        // 화면 중앙 기준으로 잘라낼 원본 비트맵 좌표 계산
        val cropSizeInBitmap = boxSize / totalScale
        val centerX = (bw / 2f) - (offset.x / totalScale)
        val centerY = (bh / 2f) - (offset.y / totalScale)

        val left = (centerX - cropSizeInBitmap / 2f).coerceIn(0f, bw - 1f).roundToInt()
        val top = (centerY - cropSizeInBitmap / 2f).coerceIn(0f, bh - 1f).roundToInt()
        val size = cropSizeInBitmap.roundToInt()
            .coerceAtMost(bitmap.width - left)
            .coerceAtMost(bitmap.height - top)
            .coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(bitmap, left, top, size, size)
        val resized = Bitmap.createScaledBitmap(cropped, 800, 800, true)

        val croppedDir = File(context.filesDir, "cropped_images").apply {
            if (!exists()) mkdirs()
        }

        val outFile = File(croppedDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { out ->
            resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        Uri.fromFile(outFile)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
/**
 * 로컬 크롭 이미지 파일(file://)만 안전하게 삭제
 * (서버 URL https://, 갤러리 원본 content:// 등은 건드리지 않음)
 */
fun deleteCroppedImageFile(context: Context, uriString: String?) {
    if (uriString.isNullOrBlank()) return
    try {
        val uri = Uri.parse(uriString)
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        val file = File(path)

        val croppedDirPath = File(context.filesDir, "cropped_images").canonicalPath
        val cacheDirPath = context.cacheDir.canonicalPath
        val filePath = file.canonicalPath

        val isManagedCroppedFile =
            filePath.startsWith(croppedDirPath + File.separator) ||
                    (filePath.startsWith(cacheDirPath + File.separator) && file.name.startsWith("cropped_"))

        if (isManagedCroppedFile && file.exists()) {
            file.delete()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}