package com.konit.hankovillage.ui.counter

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.konit.hankovillage.ui.yarn.YarnData

private val BgColor = Color(0xFFF7F4EB)
private val CardBg = Color(0xFFEFECE1)
private val TextDark = Color(0xFF332D29)
private val PointOrange = Color(0xFFD36D33)

data class MemoItem(
    val id: Long = System.currentTimeMillis(),
    val count: Int,
    var text: String
)

@Composable
fun CounterScreen(
    project: ProjectData,
    yarnList: List<YarnData> = emptyList(),
    onBackClick: () -> Unit,
    onSaveProject: (ProjectData) -> Unit = {}
) {
    var isKeepScreenOn by remember { mutableStateOf(true) }
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

    var title by remember(project.id) { mutableStateOf(project.name) }
    var info by remember(project.id) { mutableStateOf(project.needle ?: "") }
    var currentRows by remember(project.id) { mutableIntStateOf(project.currentRows) }
    var targetRows by remember(project.id) { mutableStateOf<Int?>(project.targetRows) }
    var currentStatus by remember(project.id) { mutableStateOf(project.status) }

    val memoList = remember { mutableStateListOf<MemoItem>() }

    LaunchedEffect(project.id) {
        memoList.clear()
    }

    // 변수 선언 이후에 BackHandler 배치
    BackHandler {
        val updated = project.copy(
            name = title,
            needle = info.ifBlank { null },
            currentRows = currentRows,
            targetRows = targetRows,
            status = currentStatus
        )
        onSaveProject(updated)
        onBackClick()
    }

    var showMemoDialog by remember { mutableStateOf(false) }
    var newMemoText by remember { mutableStateOf("") }

    var editingMemo by remember { mutableStateOf<MemoItem?>(null) }
    var editMemoText by remember { mutableStateOf("") }

    var memoToDelete by remember { mutableStateOf<MemoItem?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val progress = targetRows?.takeIf { it > 0 }?.let { target ->
        (currentRows.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    } ?: 0f
    val remainingCount = targetRows?.let { target -> (target - currentRows).coerceAtLeast(0) }

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
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                val updated = project.copy(
                    name = title,
                    needle = info.ifBlank { null },
                    currentRows = currentRows,
                    targetRows = targetRows,
                    status = currentStatus
                )
                onSaveProject(updated)
                onBackClick()
            }) {
                Text("< 목록으로", color = PointOrange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "작업 상태: ${if (currentStatus == "done") "완성" else "진행"}",
                    fontSize = 12.sp,
                    color = PointOrange,
                    fontWeight = FontWeight.Bold
                )
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
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
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
                    text = "$currentRows",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    color = PointOrange
                )

                if (targetRows != null) {
                    Text(text = "목표 ${targetRows}단", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

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
                    if (currentRows > 0) {
                        currentRows--
                        onSaveProject(project.copy(currentRows = currentRows))
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
                    currentRows++
                    onSaveProject(project.copy(currentRows = currentRows))
                },
                modifier = Modifier.weight(2f).height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange, contentColor = Color.White)
            ) {
                Text("+ 한 단", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                                text = if (isKeepScreenOn) "켜짐 · 뜨는 동안 화면 유지" else "꺼짐",
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
                            checkedTrackColor = PointOrange
                        )
                    )
                }
            }

            Card(
                onClick = { showMemoDialog = true },
                modifier = Modifier.height(72.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF625340)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF7DF))
            ) {
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

        Text(
            text = "메모",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (memoList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                memoList.reversed().forEach { item ->
                    Card(
                        onClick = {
                            editingMemo = item
                            editMemoText = item.text
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.count}단",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PointOrange
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.text,
                                    fontSize = 13.sp,
                                    color = TextDark
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingMemo = item
                                        editMemoText = item.text
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "메모 수정",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { memoToDelete = item },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "메모 삭제",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
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

    if (showMemoDialog) {
        AlertDialog(
            onDismissRequest = { showMemoDialog = false },
            title = { Text("메모 작성 (${currentRows}단)", fontWeight = FontWeight.Bold) },
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
                        if (newMemoText.isNotBlank()) {
                            memoList.add(MemoItem(count = currentRows, text = newMemoText))
                            newMemoText = ""
                        }
                        showMemoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("저장", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newMemoText = ""
                    showMemoDialog = false
                }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    if (editingMemo != null) {
        AlertDialog(
            onDismissRequest = { editingMemo = null },
            title = { Text("메모 수정 (${editingMemo?.count}단)", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editMemoText,
                    onValueChange = { editMemoText = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetIndex = memoList.indexOfFirst { it.id == editingMemo?.id }
                        if (targetIndex != -1 && editMemoText.isNotBlank()) {
                            memoList[targetIndex] = memoList[targetIndex].copy(text = editMemoText)
                        }
                        editingMemo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("수정 완료", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMemo = null }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    if (memoToDelete != null) {
        AlertDialog(
            onDismissRequest = { memoToDelete = null },
            title = { Text("메모 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("[${memoToDelete?.count}단] \"${memoToDelete?.text}\"\n메모를 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        memoList.remove(memoToDelete)
                        memoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { memoToDelete = null }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("단수 초기화", fontWeight = FontWeight.Bold) },
            text = { Text("현재 진행한 단수를 0단으로 초기화하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        currentRows = 0
                        onSaveProject(project.copy(currentRows = 0))
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

    if (showEditDialog) {
        var editTitle by remember { mutableStateOf(title) }
        var editInfo by remember { mutableStateOf(info) }
        var editCurrent by remember { mutableStateOf(currentRows.toString()) }
        var editTarget by remember { mutableStateOf(targetRows?.toString() ?: "") }
        var editKoreanStatus by remember { mutableStateOf(if (currentStatus == "done") "완성" else "진행") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("작품 정보 수정", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("작품 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editInfo,
                        onValueChange = { editInfo = it },
                        label = { Text("실 · 바늘 정보") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (yarnList.isNotEmpty()) {
                        Text("🧶 실 보관소에서 불러오기", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PointOrange)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            yarnList.forEach { yarn ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editInfo = yarn.name
                                        },
                                    color = CardBg,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (yarn.color.isNotBlank()) "${yarn.name} (${yarn.color})" else yarn.name,
                                        fontSize = 12.sp,
                                        color = TextDark,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editCurrent,
                        onValueChange = { editCurrent = it },
                        label = { Text("현재 단수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTarget,
                        onValueChange = { editTarget = it },
                        label = { Text("목표 단수 (선택 사항)") },
                        placeholder = { Text("미입력 시 목표 없음") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("진행 상태", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("진행", "완성").forEach { statusLabel ->
                            FilterChip(
                                selected = (editKoreanStatus == statusLabel),
                                onClick = { editKoreanStatus = statusLabel },
                                label = { Text(statusLabel) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        title = editTitle
                        info = editInfo
                        currentRows = editCurrent.toIntOrNull() ?: currentRows
                        targetRows = editTarget.toIntOrNull()
                        currentStatus = ProjectData.toServerStatus(editKoreanStatus)

                        val updatedProject = project.copy(
                            name = title,
                            needle = info.ifBlank { null },
                            currentRows = currentRows,
                            targetRows = targetRows,
                            status = currentStatus
                        )
                        onSaveProject(updatedProject)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("수정 완료", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}