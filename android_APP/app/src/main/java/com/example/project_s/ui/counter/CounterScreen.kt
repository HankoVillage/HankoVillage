package com.example.project_s.ui.counter

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.project_s.ProjectData

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

@Composable
fun CounterScreen(
    project: ProjectData,
    onBackClick: () -> Unit
) {
    // 💡 📱 [화면 켜짐 제어 상태 변수]
    var isKeepScreenOn by remember { mutableStateOf(true) }

    // 💡 스위치 상태(isKeepScreenOn)에 따라 실시간으로 화면 켜짐 플래그 켜고 끄기
    val context = LocalContext.current
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

    // 📌 실시간으로 수정 가능한 상태값들
    var title by remember { mutableStateOf(project.title) }
    var info by remember { mutableStateOf(project.info) }
    var currentCount by remember { mutableStateOf(project.currentCount) }
    var targetCount by remember { mutableStateOf(project.targetCount) }

    // 메모 작성 다이얼로그 제어
    var showMemoDialog by remember { mutableStateOf(false) }
    var newMemoText by remember { mutableStateOf("") }

    // 수정 팝업 다이얼로그 제어 변수
    var showEditDialog by remember { mutableStateOf(false) }

    // ⬇️ 초기화 경고 팝업 다이얼로그 제어 변수
    var showResetDialog by remember { mutableStateOf(false) }

    val progress = if (targetCount > 0) (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f) else 0f
    val remainingCount = (targetCount - currentCount).coerceAtLeast(0)

    // 📜 화면 전체 스크롤 상태
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
            .verticalScroll(scrollState)
            .imePadding()
    ) {
        // 1. 뒤로가기 버튼 (상단 버튼 유지)
        TextButton(onClick = onBackClick) {
            Text("< 도안 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 도안 정보 표시 및 [단수 초기화], [전체 정보 수정] 버튼 (상단 버튼 구성 유지)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "진행 중인 작업", fontSize = 12.sp, color = PointOrange, fontWeight = FontWeight.Bold)
                Text(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = info, fontSize = 13.sp, color = Color.Gray)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔄 블록형 단수 초기화 버튼
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

                // ✏️ 정보 수정 버튼
                IconButton(onClick = { showEditDialog = true }) {
                    Text("✏️", fontSize = 18.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 메인 카운터 카드
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
                    Text(text = "${remainingCount}단 남음", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 단수 조작 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (currentCount > 0) {
                        currentCount--
                        project.currentCount = currentCount
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
                    project.currentCount = currentCount
                },
                modifier = Modifier.weight(2f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange, contentColor = Color.White)
            ) {
                Text("+ 한 단", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🎨 5. 첨부 이미지와 동일한 [화면 꺼짐 방지 카드] + [메모 추가 버튼] 영역
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ☀️ 화면 꺼짐 방지 카드
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "화면 켜짐 아이콘",
                            tint = PointOrange,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "화면 꺼짐 방지",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (isKeepScreenOn) "켜짐 · 뜨는 동안 화면 유지" else "꺼짐 · 자동 화면 잠금",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Switch(
                        checked = isKeepScreenOn,
                        onCheckedChange = { isKeepScreenOn = it },
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PointOrange,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        )
                    )
                }
            }

            // ✏️ 메모 추가 버튼 카드
            Card(
                onClick = { showMemoDialog = true },
                modifier = Modifier.height(72.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp, // 선 두께
                    color = Color(0xFF625340) // 바깥쪽에 둘러씌울 테두리 색상
                ),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7DF)) // 내부 배경색
            )
             {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "메모 작성",
                        tint = TextDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "메모 추가",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🎨 6. 메모 리스트 영역 (이미지 시안 적용)
        Text(
            text = "메모",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 메모 내용이 있을 경우 목록으로 표시
        if (project.memo.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currentCount}단",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = project.memo,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }
            }
        } else {
            // 메모가 비어있을 때 안내 표시
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Text(
                    text = "작성된 메모가 없습니다. '메모 추가'를 눌러 메모를 남겨보세요.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    // 🎯 [팝업] 메모 작성 다이얼로그
    if (showMemoDialog) {
        AlertDialog(
            onDismissRequest = { showMemoDialog = false },
            title = { Text("메모 작성 (${currentCount}단)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newMemoText,
                    onValueChange = { newMemoText = it },
                    placeholder = { Text("예: 40단부터 무늬 반복 시작", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        project.memo = newMemoText
                        showMemoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("저장", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemoDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    // 🎯 [팝업] 카운터 0단 초기화 경고 대화상자
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("단수 초기화", fontWeight = FontWeight.Bold) },
            text = { Text("현재 진행한 단수를 0단으로 초기화하시겠습니까?\n이 작업은 되돌릴 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        currentCount = 0
                        project.currentCount = 0
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("초기화", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }

    // 🎯 [팝업] 도안 정보 수정 대화상자
    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(title) }
        var editInfo by remember { mutableStateOf(info) }
        var editCurrent by remember { mutableStateOf(currentCount.toString()) }
        var editTarget by remember { mutableStateOf(targetCount.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("도안 정보 수정") },
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
                        label = { Text("목표 단수") },
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
                    targetCount = editTarget.toIntOrNull() ?: targetCount

                    project.title = title
                    project.info = info
                    project.currentCount = currentCount
                    project.targetCount = targetCount

                    showEditDialog = false
                }) {
                    Text("수정 완료")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("취소") }
            }
        )
    }
}