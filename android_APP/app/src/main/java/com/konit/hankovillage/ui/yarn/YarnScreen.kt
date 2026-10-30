package com.konit.hankovillage.ui.yarn

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konit.hankovillage.ui.pattern.UriImage
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark

/**
 * 실 보관소 메인 화면
 */
@Composable
fun YarnScreen(
    yarnList: SnapshotStateList<YarnData> = remember { mutableStateListOf() }
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedYarn by remember { mutableStateOf<YarnData?>(null) }

    LaunchedEffect(Unit) {
        if (yarnList.isEmpty()) {
            yarnList.add(
                YarnData(
                    id = 1L,
                    name = "메리노 울 100",
                    color = "아이보리 / 02번",
                    shop = "바늘이야기",
                    purchaseDate = "2026-09-15",
                    memo = "가을 머플러용 3볼 구매"
                )
            )
        }
    }

    // 선택된 실이 있는 경우 전체 상세 화면 전환
    if (selectedYarn != null) {
        BackHandler {
            selectedYarn = null
        }

        YarnDetailContent(
            yarn = selectedYarn!!,
            onBackClick = { selectedYarn = null },
            onUpdate = { updatedYarn ->
                val index = yarnList.indexOfFirst { it.id == updatedYarn.id }
                if (index != -1) yarnList[index] = updatedYarn
                selectedYarn = updatedYarn
            },
            onDelete = { deletedId ->
                yarnList.removeIf { it.id == deletedId }
                selectedYarn = null
            }
        )
    } else {
        // 실 보관소 메인 목록 화면
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
                    text = "실 보관소 ",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PointOrange,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "실 등록")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (yarnList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("보관 중인 실이 없습니다.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(yarnList, key = { it.id }) { yarn ->
                        YarnCardItem(
                            yarn = yarn,
                            onClick = { selectedYarn = yarn }
                        )
                    }
                }
            }
        }
    }

    // 신규 실 등록 다이얼로그
    if (showAddDialog) {
        YarnUploadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newYarn ->
                yarnList.add(0, newYarn)
                showAddDialog = false
            }
        )
    }
}

/**
 * 목록 카드로 보여주는 실 아이템
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
            if (!yarn.imageUrl.isNullOrEmpty()) {
                UriImage(
                    uriString = yarn.imageUrl!!,
                    contentDescription = yarn.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = yarn.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1
            )

            if (yarn.color.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = PointOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = yarn.color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (yarn.shop.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "구매처: ${yarn.shop}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

/**
 * 📄 실 상세 보기 화면 (전체 화면 형태)
 */
@Composable
fun YarnDetailContent(
    yarn: YarnData,
    onBackClick: () -> Unit,
    onUpdate: (YarnData) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val detailGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            onUpdate(yarn.copy(imageUrl = it.toString()))
            Toast.makeText(context, "사진이 등록되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

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
                Text("< 실 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
            }

            Card(
                onClick = { showEditDialog = true },
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF625340)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF7DF)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
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

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { detailGalleryLauncher.launch("image/*") },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (!yarn.imageUrl.isNullOrEmpty()) 200.dp else 100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg)
        ) {
            if (!yarn.imageUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    UriImage(
                        uriString = yarn.imageUrl!!,
                        contentDescription = yarn.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "사진 변경",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = PointOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "실 사진 등록하기",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = yarn.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDark)

        if (yarn.color.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = PointOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = yarn.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PointOrange,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("구매처: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text(if (yarn.shop.isNotBlank()) yarn.shop else "미입력", fontSize = 14.sp, color = TextDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("구매일: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text(if (yarn.purchaseDate.isNotBlank()) yarn.purchaseDate else "미입력", fontSize = 14.sp, color = TextDark)
                }

                if (yarn.memo.isNotBlank()) {
                    Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)
                    Text("메모", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Text(yarn.memo, fontSize = 13.sp, color = TextDark)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
        ) {
            Text("실 정보 삭제하기")
        }
    }

    if (showEditDialog) {
        YarnEditDialog(
            yarn = yarn,
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
            title = { Text("실 정보 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("\"${yarn.name}\" 실 정보를 삭제하시겠습니까?") },
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

/**
 * 신규 실 등록 다이얼로그
 */
@Composable
fun YarnUploadDialog(
    onDismiss: () -> Unit,
    onConfirm: (YarnData) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 실 등록", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (!imageUri.isNullOrEmpty()) {
                        UriImage(
                            uriString = imageUri!!,
                            contentDescription = "선택한 실 사진",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PointOrange)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("실 사진 추가 (선택)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("실 이름 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("색상 / 번호 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = { Text("구매처 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("구매일 (선택)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모 (선택)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            YarnData(
                                name = name,
                                color = color,
                                shop = shop,
                                purchaseDate = purchaseDate,
                                memo = memo,
                                imageUrl = imageUri
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
 * 실 정보 수정 다이얼로그
 */
@Composable
fun YarnEditDialog(
    yarn: YarnData,
    onDismiss: () -> Unit,
    onConfirm: (YarnData) -> Unit
) {
    var name by remember { mutableStateOf(yarn.name) }
    var color by remember { mutableStateOf(yarn.color) }
    var shop by remember { mutableStateOf(yarn.shop) }
    var purchaseDate by remember { mutableStateOf(yarn.purchaseDate) }
    var memo by remember { mutableStateOf(yarn.memo) }
    var imageUri by remember { mutableStateOf(yarn.imageUrl) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { imageUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("실 정보 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (!imageUri.isNullOrEmpty()) {
                        UriImage(
                            uriString = imageUri!!,
                            contentDescription = "수정된 실 사진",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PointOrange)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("사진 변경하기", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("실 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = { Text("색상 / 번호") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shop,
                    onValueChange = { shop = it },
                    label = { Text("구매처") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { purchaseDate = it },
                    label = { Text("구매일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            yarn.copy(
                                name = name,
                                color = color,
                                shop = shop,
                                purchaseDate = purchaseDate,
                                memo = memo,
                                imageUrl = imageUri
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