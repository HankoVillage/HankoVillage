package com.example.project_s.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.project_s.ui.counter.BgColor
import com.example.project_s.ui.counter.CardBg
import com.example.project_s.ui.counter.PointOrange
import com.example.project_s.ui.counter.TextDark

@Composable
fun ProjectListScreen(
    projectList: List<ProjectData>,
    onAddProject: (ProjectData) -> Unit,
    onDeleteProject: (ProjectData) -> Unit, // 🗑️ 삭제 동작 콜백 추가
    onProjectSelect: (ProjectData) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
    ) {
        // 1. 헤더 (타이틀 + 새 도안 추가 버튼)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "내 카운터 목록 🧶", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = "작업할 카드를 선택하세요", fontSize = 13.sp, color = Color.Gray)
            }
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ 새 카운터", fontSize = 13.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 카드 2열 배치
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projectList) { project ->
                ProjectCardItem(
                    project = project,
                    onClick = { onProjectSelect(project) },
                    onDelete = { onDeleteProject(project) } // 🗑️ 삭제 함수 전달
                )
            }
        }
    }

    // 🎯 [팝업] 새 도안 추가 대화상자
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var info by remember { mutableStateOf("") }
        var needleType by androidx.compose.runtime.remember {mutableStateOf("")}
        var current by remember { mutableStateOf("0") }
        var target by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 단수ㅜ 카운터 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("작품 이름 (예: 가을 머플러)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = needleType,
                        onValueChange = { needleType = it },
                        label = { Text("바늘 종류 (예: 대바늘 / 코바늘)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = info,
                        onValueChange = { info = it },
                        label = { Text("실 · 바늘 정보 (예: 울 / 4mm)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it },
                        label = { Text("시작 단수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = target, onValueChange = { target = it },
                        label = { Text("목표 단수") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotEmpty()) {
                        val newProject = ProjectData(
                            title = title,
                            needleType = needleType,
                            info = if (info.isEmpty()) "정보 없음" else info,
                            currentCount = current.toIntOrNull() ?: 0,
                            targetCount = target.toIntOrNull() ?: 100
                        )
                        onAddProject(newProject)
                        showAddDialog = false
                    }
                }) {
                    Text("추가하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            }
        )
    }
}

// 📌 개별 카드 아이템 (삭제 버튼 및 팝업 포함)
@Composable
fun ProjectCardItem(
    project: ProjectData,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 상단: 상태 태그 + 쓰레기통 삭제 아이콘
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (project.status == "진행") Color(0xFFFDF0E9) else Color(0xFFE3EAD8),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = project.status,
                        fontSize = 11.sp,
                        color = PointOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                // 🗑️ 삭제 아이콘 버튼
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "도안 삭제",
                        tint = Color.Gray
                    )
                }
            }

            // 하단: 타이틀, 정보, 단수
            Column {
                Text(text = project.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = project.info, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${project.currentCount} / ${project.targetCount}단", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PointOrange)
            }
        }
    }

    // 🗑️ [삭제 확인 팝업]
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("도안 삭제") },
            text = { Text("'${project.title}' 도안을 완전히 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소")
                }
            }
        )
    }
}