package com.konit.hankovillage.ui.yarn

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

/**
 * 실 보관소의 목록 조회, 상세 정보 확인 및 신규 실 등록을 처리하는 화면 컴포저블
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarnScreen() {
    val yarnList = remember {
        mutableStateListOf(
            YarnData(
                id = 1L,
                name = "메리노 울 100",
                color = "아이보리 / 02번",
                shop = "바늘이야기",
                purchaseDate = "2026-09-15",
                memo = "가을 머플러용으로 3볼 구매. 권장 바늘 4.0mm"
            )
        )
    }

    var selectedYarn by remember { mutableStateOf<YarnData?>(null) }
    var isNewYarn by remember { mutableStateOf(false) }

    if (selectedYarn != null) {
        BackHandler {
            selectedYarn = null
            isNewYarn = false
        }

        YarnDetailContent(
            yarn = selectedYarn!!,
            isNew = isNewYarn,
            onBackClick = {
                selectedYarn = null
                isNewYarn = false
            },
            onSave = { savedYarn ->
                val index = yarnList.indexOfFirst { it.id == savedYarn.id }
                if (index != -1) {
                    yarnList[index] = savedYarn
                } else {
                    yarnList.add(0, savedYarn)
                }
                selectedYarn = null
                isNewYarn = false
            },
            onDelete = { deletedId ->
                yarnList.removeIf { it.id == deletedId }
                selectedYarn = null
                isNewYarn = false
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
                    text = "실 보관소",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                FloatingActionButton(
                    onClick = {
                        selectedYarn = YarnData(name = "", color = "")
                        isNewYarn = true
                    },
                    containerColor = PointOrange,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "새 실 추가")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (yarnList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("보관 중인 실이 없습니다. + 버튼을 눌러 추가해 보세요!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(yarnList, key = { it.id }) { item ->
                        YarnCardItem(
                            yarn = item,
                            onClick = {
                                selectedYarn = item
                                isNewYarn = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 실 보관소 그리드 내 개별 실 정보를 표시하는 카드 컴포저블
 *
 * @param yarn 카드에 표시할 실 데이터 객체
 * @param onClick 카드 선택 시 수행할 동작
 */
@Composable
fun YarnCardItem(
    yarn: YarnData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFFDCD7C9), shape = RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("실 사진", fontSize = 12.sp, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (yarn.name.isNotBlank()) yarn.name else "이름 없음",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (yarn.color.isNotBlank()) {
                Text(
                    text = yarn.color,
                    fontSize = 12.sp,
                    color = PointOrange,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            if (yarn.shop.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = yarn.shop,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 실의 세부 정보(이름, 색상, 구매 정보, 메모) 수정 및 작성을 처리하는 상세 화면 컴포저블
 *
 * @param yarn 작성/수정할 대상 실 데이터
 * @param isNew 신규 등록 여부 (true인 경우 삭제 버튼 숨김 처리)
 * @param onBackClick 뒤로가기 클릭 콜백
 * @param onSave 데이터 저장 확정 콜백
 * @param onDelete 실 데이터 삭제 확정 콜백
 */
@Composable
fun YarnDetailContent(
    yarn: YarnData,
    isNew: Boolean,
    onBackClick: () -> Unit,
    onSave: (YarnData) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf(yarn.name) }
    var color by remember { mutableStateOf(yarn.color) }
    var shop by remember { mutableStateOf(yarn.shop) }
    var purchaseDate by remember { mutableStateOf(yarn.purchaseDate) }
    var memo by remember { mutableStateOf(yarn.memo) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("< 실 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && color.isNotBlank()) {
                        onSave(
                            yarn.copy(
                                name = name,
                                color = color,
                                shop = shop,
                                purchaseDate = purchaseDate,
                                memo = memo
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                enabled = name.isNotBlank() && color.isNotBlank()
            ) {
                Text(if (isNew) "등록 저장" else "수정 저장", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "사진 변경",
                            tint = PointOrange,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text("실 사진 등록 / 수정 (클릭)", fontSize = 13.sp, color = Color.Gray)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("실 이름 *") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PointOrange)
            )

            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("색상 *") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PointOrange)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = shop,
                onValueChange = { shop = it },
                label = { Text("구매처") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PointOrange)
            )

            OutlinedTextField(
                value = purchaseDate,
                onValueChange = { purchaseDate = it },
                label = { Text("구매일 (예: 2026-10-05)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PointOrange)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEA))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("실 상세 메모", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    placeholder = { Text("볼 수, 소모량, 권장 바늘, 뜨개 팁 등을 자유롭게 적어두세요", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PointOrange,
                        unfocusedBorderColor = Color(0xFFE6DFCE)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        if (!isNew) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
            ) {
                Text("실 삭제하기")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("실 삭제", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${yarn.name}\" 실을 삭제하시겠습니까?\n\n⚠ 이 실을 쓰는 카운터는 실 연결이 해제돼요.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(yarn.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}