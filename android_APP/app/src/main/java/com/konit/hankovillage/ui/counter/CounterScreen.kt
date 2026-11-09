package com.konit.hankovillage.ui.counter

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konit.hankovillage.ui.yarn.YarnData

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

/**
 * 카운터 단수별 메모 항목 데이터 모델입니다.
 *
 * @property id 메모의 고유 식별자
 * @property count 메모가 작성된 시점의 진행 단수
 * @property text 메모 내용
 */
data class MemoItem(
    val id: Long = System.currentTimeMillis(),
    val count: Int,
    var text: String
)

/**
 * 뜨개질 프로젝트의 진행 단수 카운팅 및 메모 관리 화면 컴포저블입니다.
 *
 * @property project 카운터 화면에서 바인딩할 프로젝트 상세 데이터
 * @property onBackClick 이전 화면 이동 콜백
 * @property onSaveProject 프로젝트 변경 사항(단수, 수정 정보) 저장 콜백
 */
@Composable
fun CounterScreen(
    project: ProjectData,
    yarnList: List<YarnData> = emptyList(),
    onBackClick: () -> Unit,
    onSaveProject: (ProjectData) -> Unit = {}
) {
    BackHandler {
        onBackClick()
    }

    var isKeepScreenOn by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // 뜨개질 작업 중 화면이 켜진 상태를 유지해야 하므로 Activity Window Flag 제어
    DisposableEffect(isKeepScreenOn) {
        val window = (context as? Activity)?.window
        if (isKeepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var title by remember(project.id) { mutableStateOf(project.name) }
    var info by remember(project.id) { mutableStateOf(project.needle ?: "") }
    var currentCount by remember(project.id) { mutableIntStateOf(project.currentRows) }
    var targetCount by remember(project.id) { mutableStateOf<Int?>(project.targetRows) }

    val memoList = remember { mutableStateListOf<MemoItem>() }

    LaunchedEffect(project.id) {
        memoList.clear()
    }

    var showMemoDialog by remember { mutableStateOf(false) }
    var newMemoText by remember { mutableStateOf("") }

    var editingMemo by remember { mutableStateOf<MemoItem?>(null) }
    var editMemoText by remember { mutableStateOf("") }

    var memoToDelete by remember { mutableStateOf<MemoItem?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val progress = targetCount?.takeIf { it > 0 }?.let { target ->
        (currentCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    } ?: 0f
    val remainingCount = targetCount?.let { target -> (target - currentCount).coerceAtLeast(0) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("< 취소", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val updatedProject = project.copy(
                        name = title,
                        needle = info.ifBlank { null },
                        currentRows = currentCount,
                        targetRows = targetCount
                    )
                    onSaveProject(updatedProject)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("저장하기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "작업 상태: ${project.getStatusDisplay()}", fontSize = 12.sp, color = PointOrange, fontWeight = FontWeight.Bold)
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                if (info.isNotBlank()) {
                    Text(text = info, fontSize = 13.sp, color = Color.Gray)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PointOrange),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PointOrange.copy(alpha = 0.08f)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "단수 초기화",
                            tint = PointOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "초기화",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PointOrange
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(
                        text = "수정",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "현재 단수", fontSize = 14.sp, color = TextDark)
                Text(
                    text = "$currentCount",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = PointOrange
                )

                if (targetCount != null) {
                    Text(text = "목표 ${targetCount}단", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = PointOrange,
                        trackColor = Color(0xFFDCD7C9)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${(progress * 100).toInt()}% 완료", fontSize = 12.sp, color = Color.Gray)
                        if (remainingCount != null) {
                            Text(text = "${remainingCount}단 남음", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Text(text = "목표 없음", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (currentCount > 0) {
                        currentCount--
                        onSaveProject(project.copy(currentRows = currentCount))
                    }
                },
                modifier = Modifier.weight(1f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardBg, contentColor = TextDark)
            ) {
                Text("—", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    currentCount++
                    onSaveProject(project.copy(currentRows = currentCount))
                },
                modifier = Modifier.weight(2f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange, contentColor = Color.White)
            ) {
                Text("+ 한 단", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(title) }
        var editInfo by remember { mutableStateOf(info) }
        var editCurrent by remember { mutableStateOf(currentCount.toString()) }
        var editTarget by remember { mutableStateOf(targetCount?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("카운터 정보 수정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("작품 이름") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = editInfo,
                        onValueChange = { editInfo = it },
                        label = { Text("실 · 바늘 정보") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = editCurrent, onValueChange = { editCurrent = it },
                        label = { Text("현재 단수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editTarget, onValueChange = { editTarget = it },
                        label = { Text("목표 단수 (선택 사항)") },
                        placeholder = { Text("미입력 시 목표 없음") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    title = editTitle
                    info = editInfo
                    currentCount = editCurrent.toIntOrNull() ?: currentCount

                    // 사용자가 목표 단수를 입력하지 않으면 null 처리하여 "목표 없음"으로 전환
                    targetCount = editTarget.toIntOrNull()

                    val updatedProject = project.copy(
                        name = title,
                        needle = info.ifBlank { null },
                        currentRows = currentCount,
                        targetRows = targetCount
                    )
                    onSaveProject(updatedProject)
                    showEditDialog = false
                }) {
                    Text("수정 완료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}