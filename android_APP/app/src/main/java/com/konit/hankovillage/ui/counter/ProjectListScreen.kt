package com.konit.hankovillage.ui.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.foundation.lazy.grid.items

/**
 * 등록된 프로젝트 카운터 목록을 조회하고, 신규 카운터를 추가하거나 선택/삭제하는 화면 컴포저블
 *
 * @param projectList 등록된 프로젝트 목록
 * @param onAddProject 프로젝트 추가 콜백
 * @param onDeleteProject 프로젝트 삭제 콜백
 * @param onProjectSelect 프로젝트 선택 콜백
 */
@Composable
fun ProjectListScreen(
    projectList: List<ProjectData>,
    onAddProject: (ProjectData) -> Unit,
    onDeleteProject: (ProjectData) -> Unit,
    onProjectSelect: (ProjectData) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "내 카운터 목록", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(projectList) { project ->
                ProjectCardItem(
                    project = project,
                    onClick = { onProjectSelect(project) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var info by remember { mutableStateOf("") }
        var needleType by remember { mutableStateOf("") }
        var current by remember { mutableStateOf("0") }
        var target by remember { mutableStateOf("100") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 단수 카운터 추가") },
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
                        value = target,
                        onValueChange = { target = it },
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

/**
 * 개별 카운터 카드 항목 컴포저블
 *
 * @param project 카드에 표시할 프로젝트 데이터
 * @param onClick 카드 영역 선택 콜백
 * @param onDelete 카드 삭제 클릭 콜백
 */
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

            Column {
                Text(text = project.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text(text = project.info, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${project.currentCount} / ${project.targetCount}단",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PointOrange
                )
            }
        }
    }

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