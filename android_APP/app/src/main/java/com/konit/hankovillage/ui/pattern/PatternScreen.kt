package com.konit.hankovillage.ui.pattern

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

val AvailableTags = listOf("의류", "목도리", "모자", "가방", "인형", "기타")

/**
 * 도안 보관소의 전체 목록 검색, 필터링, 상세 보기 및 등록 프로세스를 관리하는 메인 화면 컴포저블
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternScreen() {
    val patternList = remember {
        mutableStateListOf(
            PatternData(
                id = 1L,
                title = "가을 체크 머플러",
                tag = "목도리",
                yarnInfo = "메리노울 100g",
                needleInfo = "4.0mm 대바늘",
                patternUrl = "https://example.com/pattern/1",
                status = PatternStatus.IN_PROGRESS
            ),
            PatternData(
                id = 2L,
                title = "탑다운 여름 니트",
                tag = "의류",
                yarnInfo = "코튼 실 200g",
                needleInfo = "3.5mm 줄바늘",
                patternUrl = "",
                status = PatternStatus.PLANNED
            )
        )
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf<PatternStatus?>(null) }
    var selectedSortOrder by rememberSaveable { mutableStateOf(PatternSortOrder.NEWEST) }

    var selectedPattern by remember { mutableStateOf<PatternData?>(null) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }

    val filteredPatterns = patternList.filter { pattern ->
        val matchesQuery = if (searchQuery.trim().length >= 2) {
            pattern.title.contains(searchQuery, ignoreCase = true) ||
                    pattern.tag.contains(searchQuery, ignoreCase = true) ||
                    pattern.yarnInfo.contains(searchQuery, ignoreCase = true)
        } else true

        val matchesStatus = selectedStatusFilter?.let { pattern.status == it } ?: true
        matchesQuery && matchesStatus
    }.sortedWith { p1, p2 ->
        if (selectedSortOrder == PatternSortOrder.NEWEST) p2.id.compareTo(p1.id)
        else p1.title.compareTo(p2.title)
    }

    if (selectedPattern != null) {
        PatternDetailContent(
            pattern = selectedPattern!!,
            onBackClick = { selectedPattern = null },
            onUpdate = { updated ->
                val index = patternList.indexOfFirst { it.id == updated.id }
                if (index != -1) patternList[index] = updated
                selectedPattern = updated
            },
            onDelete = { deletedId ->
                patternList.removeIf { it.id == deletedId }
                selectedPattern = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgColor)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "도안 보관소",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = PointOrange,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "도안 등록")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("제목, 태그, 실 정보 검색 (2글자 이상)", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PointOrange,
                    unfocusedBorderColor = Color.LightGray
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("전체", fontSize = 11.sp) }
                    )
                    PatternStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = {
                                selectedStatusFilter = if (selectedStatusFilter == status) null else status
                            },
                            label = { Text(status.label, fontSize = 11.sp) }
                        )
                    }
                }

                TextButton(onClick = {
                    selectedSortOrder = if (selectedSortOrder == PatternSortOrder.NEWEST) {
                        PatternSortOrder.TITLE
                    } else {
                        PatternSortOrder.NEWEST
                    }
                }) {
                    Text(text = "정렬: ${selectedSortOrder.label}", fontSize = 12.sp, color = PointOrange)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPatterns.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("보관된 도안이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPatterns, key = { it.id }) { item ->
                        PatternCardItem(
                            pattern = item,
                            onClick = { selectedPattern = item }
                        )
                    }
                }
            }
        }
    }

    if (showUploadDialog) {
        PatternUploadDialog(
            onDismiss = { showUploadDialog = false },
            onConfirm = { newPattern ->
                patternList.add(0, newPattern)
                showUploadDialog = false
            }
        )
    }
}

/**
 * 그리드 목록 내 개별 도안 정보를 카드 형태로 보여주는 컴포저블
 *
 * @param pattern 표시할 도안 데이터
 * @param onClick 카드 선택 시 수행할 행동
 */
@Composable
fun PatternCardItem(
    pattern: PatternData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = when (pattern.status) {
                        PatternStatus.PLANNED -> Color(0xFFE0E0E0)
                        PatternStatus.IN_PROGRESS -> PointOrange.copy(alpha = 0.2f)
                        PatternStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = pattern.status.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pattern.status == PatternStatus.IN_PROGRESS) PointOrange else TextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(text = "#${pattern.tag}", fontSize = 11.sp, color = PointOrange, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pattern.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (pattern.yarnInfo.isNotBlank()) {
                Text(text = "사용 실: ${pattern.yarnInfo}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
            if (pattern.needleInfo.isNotBlank()) {
                Text(text = "바늘 정보: ${pattern.needleInfo}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

/**
 * 선택된 도안의 세부 정보를 표시하고 수정/삭제 및 외부 링크 이동을 제공하는 상세 화면
 *
 * @param pattern 상세 정보를 출력할 도안 객체
 * @param onBackClick 뒤로가기 버튼 클릭 콜백
 * @param onUpdate 도안 정보 업데이트 발생 시 콜백
 * @param onDelete 도안 삭제 요청 시 콜백
 */
@Composable
fun PatternDetailContent(
    pattern: PatternData,
    onBackClick: () -> Unit,
    onUpdate: (PatternData) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

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
            TextButton(onClick = onBackClick) {
                Text("< 도안 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = { showEditDialog = true }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "도안 수정", tint = TextDark)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = PointOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "#${pattern.tag}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PointOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(text = pattern.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "진행 상태", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PatternStatus.entries.forEach { statusOption ->
                val isSelected = pattern.status == statusOption
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onUpdate(pattern.copy(status = statusOption))
                    },
                    label = { Text(statusOption.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PointOrange,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("사용 실: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text(if (pattern.yarnInfo.isNotBlank()) pattern.yarnInfo else "미입력", fontSize = 14.sp, color = TextDark)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("바늘 정보: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text(if (pattern.needleInfo.isNotBlank()) pattern.needleInfo else "미입력", fontSize = 14.sp, color = TextDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pattern.patternUrl.isNotBlank()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pattern.patternUrl))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("도안 링크 열기", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(
                onClick = { showEditDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("도안 링크 등록하기", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Text("도안 삭제하기")
        }
    }

    if (showEditDialog) {
        PatternEditDialog(
            pattern = pattern,
            onDismiss = { showEditDialog = false },
            onConfirm = { updated ->
                onUpdate(updated)
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("도안 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${pattern.title}\" 도안을 정말 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(pattern.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}

/**
 * 신규 도안 등록을 위한 모달 다이얼로그
 *
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 도안 생성 확정 시 콜백
 */
@Composable
fun PatternUploadDialog(
    onDismiss: () -> Unit,
    onConfirm: (PatternData) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(AvailableTags.first()) }
    var yarnInfo by remember { mutableStateOf("") }
    var needleInfo by remember { mutableStateOf("") }
    var patternUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 도안 등록", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("도안 제목 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("태그 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvailableTags.forEach { tagOption ->
                        FilterChip(
                            selected = selectedTag == tagOption,
                            onClick = { selectedTag = tagOption },
                            label = { Text(tagOption, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = yarnInfo,
                    onValueChange = { yarnInfo = it },
                    label = { Text("사용 실 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = needleInfo,
                    onValueChange = { needleInfo = it },
                    label = { Text("바늘 정보 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = patternUrl,
                    onValueChange = { patternUrl = it },
                    label = { Text("도안 링크 URL (선택)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            PatternData(
                                title = title,
                                tag = selectedTag,
                                yarnInfo = yarnInfo,
                                needleInfo = needleInfo,
                                patternUrl = patternUrl
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("등록", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        }
    )
}

/**
 * 기존 도안 정보의 수정을 처리하는 모달 다이얼로그
 *
 * @param pattern 수정할 대상 도안 객체
 * @param onDismiss 다이얼로그 닫기 콜백
 * @param onConfirm 수정 사항 적용 확정 콜백
 */
@Composable
fun PatternEditDialog(
    pattern: PatternData,
    onDismiss: () -> Unit,
    onConfirm: (PatternData) -> Unit
) {
    var title by remember { mutableStateOf(pattern.title) }
    var selectedTag by remember { mutableStateOf(pattern.tag) }
    var yarnInfo by remember { mutableStateOf(pattern.yarnInfo) }
    var needleInfo by remember { mutableStateOf(pattern.needleInfo) }
    var patternUrl by remember { mutableStateOf(pattern.patternUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("도안 정보 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("도안 제목") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("태그 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvailableTags.forEach { tagOption ->
                        FilterChip(
                            selected = selectedTag == tagOption,
                            onClick = { selectedTag = tagOption },
                            label = { Text(tagOption, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = yarnInfo,
                    onValueChange = { yarnInfo = it },
                    label = { Text("사용 실") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = needleInfo,
                    onValueChange = { needleInfo = it },
                    label = { Text("바늘 정보") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = patternUrl,
                    onValueChange = { patternUrl = it },
                    label = { Text("도안 링크 URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            pattern.copy(
                                title = title,
                                tag = selectedTag,
                                yarnInfo = yarnInfo,
                                needleInfo = needleInfo,
                                patternUrl = patternUrl
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("수정 완료", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        }
    )
}