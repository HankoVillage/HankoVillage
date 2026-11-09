package com.konit.hankovillage.ui.counter

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
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark

/**
 * 등록된 프로젝트 카운터 목록을 조회하고, 신규 카운터를 추가하거나 선택/삭제하는 화면 컴포저블
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

        if (projectList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 카운터가 없습니다.\n'+ 새 카운터'를 눌러 시작해보세요!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(projectList, key = { it.id }) { project ->
                    ProjectCardItem(
                        project = project,
                        onClick = { onProjectSelect(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var selectedKoreanNeedle by remember { mutableStateOf("대바늘") }
        var needleInput by remember { mutableStateOf("") }
        var targetRowInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 단수 카운터 추가", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { if (it.length <= 50) nameInput = it },
                        label = { Text("작품 이름 (예: 가을 머플러)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "바늘 종류",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    val needleOptions = listOf("대바늘", "코바늘", "둘 다")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        needleOptions.forEach { option ->
                            FilterChip(
                                selected = (selectedKoreanNeedle == option),
                                onClick = { selectedKoreanNeedle = option },
                                label = { Text(option) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PointOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = needleInput,
                        onValueChange = { if (it.length <= 50) needleInput = it },
                        label = { Text("실 · 바늘 상세 정보 (예: 메리노울 / 4.0mm)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetRowInput,
                        onValueChange = { targetRowInput = it.filter { char -> char.isDigit() } },
                        label = { Text("목표 단수 (선택 사항)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            val serverNeedleType = ProjectData.toServerNeedleType(selectedKoreanNeedle)
                            val targetRows = targetRowInput.toIntOrNull()

                            val newProject = ProjectData(
                                id = System.currentTimeMillis(),
                                name = nameInput,
                                needleType = serverNeedleType,
                                needle = needleInput.ifBlank { null },
                                currentRows = 0,
                                targetRows = targetRows,
                                status = "진행"
                            )

                            onAddProject(newProject)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("추가하기", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}

/**
 * 개별 카운터 카드 항목 컴포저블
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
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isProgress = project.status == "진행"
                Surface(
                    color = if (isProgress) Color(0xFFFDF0E9) else Color(0xFFE3EAD8),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = project.status,
                        fontSize = 11.sp,
                        color = if (isProgress) PointOrange else Color(0xFF4CAF50),
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
                        contentDescription = "카운터 삭제",
                        tint = Color.Gray
                    )
                }
            }

            Column {
                Text(
                    text = project.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1
                )
                Text(
                    text = project.needle ?: "",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                val targetText = project.targetRows?.let { "${it}단" } ?: "자율"
                Text(
                    text = "${project.currentRows} / $targetText",
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
            title = { Text("카운터 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("'${project.name}' 카운터를 완전히 삭제하시겠습니까?") },
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
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}