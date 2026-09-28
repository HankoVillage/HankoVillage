package com.example.project_s.ui.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
// ⬇️ 초기화 아이콘(Refresh) 임포트 추가
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    // 📌 실시간으로 수정 가능한 상태값들
    var title by remember { mutableStateOf(project.title) }
    var info by remember { mutableStateOf(project.info) }
    var currentCount by remember { mutableStateOf(project.currentCount) }
    var targetCount by remember { mutableStateOf(project.targetCount) }
    var memoText by remember { mutableStateOf(project.memo) } // 📝 메모 상태 관리

    // 수정 팝업 다이얼로그 제어 변수
    var showEditDialog by remember { mutableStateOf(false) }

    // ⬇️ 초기화 경고 팝업 다이얼로그 제어 변수 추가
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
            // ⬇️ 메모가 추가되어 키보드가 올라와도 스크롤 가능하도록 추가
            .verticalScroll(scrollState)
            // ⬇️ 키보드가 올라오는 높이만큼 아래쪽에 자동으로 여백(Padding)을 줍니다!
            .imePadding()
    ) {
        // 1. 뒤로가기 버튼
        TextButton(onClick = onBackClick) {
            Text("< 도안 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. 도안 정보 표시 및 [전체 정보 수정] 버튼
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
                // 🔄 1. 눈에 잘 띄는 블록형 단수 초기화 버튼
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PointOrange),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PointOrange.copy(alpha = 0.08f) // 은은하게 채워진 배경색
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

                // ✏️ 2. 정보 수정 버튼
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

        // 5. 📝 뜨개 메모장 카드 영역 추가[cite: 1, 2]
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "메모 아이콘",
                        tint = PointOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "뜨개 메모 📝", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = memoText,
                    onValueChange = {
                        memoText = it
                        project.memo = it // 💡 작성하는 즉시 원본 ProjectData 객체의 memo에 저장[cite: 1, 2]
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    placeholder = {
                        Text(
                            "특이사항이나 무늬 변경 단수를 메모하세요.\n(예: 10단마다 코늘림하기)",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PointOrange,
                        unfocusedBorderColor = Color(0xFFDCD7C9),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }
        }
    }

    // ⬇️ 🎯 [팝업] 카운터 0단 초기화 경고 대화상자 추가
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text("단수 초기화", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("현재 진행한 단수를 0단으로 초기화하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        // ⬇️ 단수를 0으로 덮어쓰고 원본 데이터에도 반영
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
                TextButton(onClick = { showResetDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    // 🎯 [팝업] 사용자가 모든 정보를 재수정하는 대화상자
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

                    // 원본 데이터 객체 갱신
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