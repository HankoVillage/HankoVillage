package com.konit.hankovillage.ui.yarn

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konit.hankovillage.network.HankoApiService
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SessionManager
import com.konit.hankovillage.ui.counter.ProjectData
import com.konit.hankovillage.ui.pattern.ImageUploadResponse
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark
import com.konit.hankovillage.ui.util.ImageCropDialog
import com.konit.hankovillage.ui.util.deleteCroppedImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

enum class YarnSortOrder(val label: String, val serverSort: String) {
    NEWEST("최신순", "NEWEST"),
    NAME("이름순", "NAME")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YarnScreen(
    yarnList: SnapshotStateList<YarnData> = remember { mutableStateListOf() },
    projectList: SnapshotStateList<ProjectData>? = null,
    isLoggedIn: Boolean = true,
    apiService: HankoApiService? = null
) {
    val context = LocalContext.current
    val resolvedApiService = remember(apiService, context) {
        apiService ?: RetrofitInstance.getApiService(context)
    }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(YarnSortOrder.NEWEST) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedYarn by remember { mutableStateOf<YarnData?>(null) }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var yarnToEdit by remember { mutableStateOf<YarnData?>(null) }
    var yarnToDelete by remember { mutableStateOf<YarnData?>(null) }

    fun fetchYarns(targetSort: YarnSortOrder = sortOrder) {
        if (!isLoggedIn) return
        coroutineScope.launch {
            isLoading = true
            try {
                val response = resolvedApiService.getYarns(
                    sort = targetSort.serverSort,
                    limit = 100,
                    offset = 0
                )
                if (response.isSuccessful) {
                    val serverItems = response.body().orEmpty()
                    yarnList.clear()
                    yarnList.addAll(serverItems)
                } else {
                    Toast.makeText(context, "실 목록을 불러오지 못했어요 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "네트워크 연결을 확인해 주세요", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(isLoggedIn, sortOrder) {
        if (isLoggedIn) {
            fetchYarns(sortOrder)
        }
    }

    fun performDeleteYarn(target: YarnData) {
        coroutineScope.launch {
            try {
                val response = resolvedApiService.deleteYarn(target.id)
                if (response.isSuccessful || response.code() == 204) {
                    deleteCroppedImageFile(context, target.imageUrl)
                    deleteCroppedImageFile(context, target.thumbnailUrl)
                    yarnList.removeIf { it.id == target.id }
                    projectList?.indices?.forEach { idx ->
                        if (projectList[idx].yarnId == target.id) {
                            projectList[idx] = projectList[idx].copy(yarnId = null)
                        }
                    }
                    if (selectedYarn?.id == target.id) {
                        selectedYarn = null
                    }
                    Toast.makeText(context, "실이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "실 삭제에 실패했어요 (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                Toast.makeText(context, "네트워크 연결을 확인해 주세요", Toast.LENGTH_SHORT).show()
            } finally {
                yarnToDelete = null
            }
        }
    }

    val filteredYarns = remember(yarnList.toList(), searchQuery) {
        if (searchQuery.isBlank()) {
            yarnList.toList()
        } else {
            val q = searchQuery.trim().lowercase()
            yarnList.filter {
                it.name.lowercase().contains(q) ||
                        (it.color?.lowercase()?.contains(q) == true) ||
                        (it.shop?.lowercase()?.contains(q) == true) ||
                        (it.memo?.lowercase()?.contains(q) == true)
            }
        }
    }

    if (selectedYarn != null) {
        BackHandler { selectedYarn = null }

        YarnDetailContent(
            yarn = selectedYarn!!,
            onBackClick = { selectedYarn = null },
            onEditClick = { yarnToEdit = selectedYarn },
            onDeleteClick = { yarnToDelete = selectedYarn }
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
                Column {
                    Text("실 보관소", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("보유 중인 실 ${yarnList.size}개", fontSize = 12.sp, color = Color.Gray)
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("실 등록", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("실 이름, 색상, 구매처, 메모 검색", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "검색어 지우기", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YarnSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = sortOrder == order,
                        onClick = { sortOrder = order },
                        label = { Text(order.label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading && yarnList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PointOrange)
                }
            } else if (filteredYarns.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank()) "보관 중인 실이 없습니다. 새 실을 등록해 보세요!" else "검색 결과가 없습니다.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredYarns, key = { it.id }) { item ->
                        YarnCardItem(
                            yarn = item,
                            onClick = { selectedYarn = item },
                            onEditClick = { yarnToEdit = item },
                            onDeleteClick = { yarnToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        YarnFormDialog(
            initialYarn = null,
            titleText = "새 실 등록",
            confirmText = "등록하기",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, shop, purchaseDate, memo, localImageUri, _ ->
                coroutineScope.launch {
                    try {
                        val createReq = YarnCreateRequest(
                            name = name.trim().take(50),
                            color = color.trim().take(30).ifBlank { null },
                            purchaseSite = shop.trim().take(100).ifBlank { null },
                            purchaseDate = purchaseDate.trim().ifBlank { null },
                            memo = memo.trim().take(100).ifBlank { null }
                        )
                        val createRes = resolvedApiService.createYarn(createReq)
                        if (!createRes.isSuccessful || createRes.body() == null) {
                            deleteCroppedImageFile(context, localImageUri)
                            val msg = if (createRes.code() == 422) "입력값 형식(날짜 YYYY-MM-DD 등)을 확인해 주세요"
                            else "실 등록에 실패했어요 (${createRes.code()})"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        var createdYarn = createRes.body()!!

                        val localFile = uriStringToLocalFile(localImageUri)
                        if (localFile != null && localFile.exists()) {
                            val part = createImageMultipart(localFile)
                            val imgRes = resolvedApiService.uploadYarnImage(createdYarn.id, part)
                            if (imgRes.isSuccessful && imgRes.body() != null) {
                                // ImageUploadResponse(image_url, thumbnail_url)만 받아 기존 YarnData에 반영
                                val uploaded: ImageUploadResponse = imgRes.body()!!
                                createdYarn = createdYarn.copy(
                                    imageUrl = uploaded.imageUrl ?: createdYarn.imageUrl,
                                    thumbnailUrl = uploaded.thumbnailUrl ?: createdYarn.thumbnailUrl
                                )
                            } else if (imgRes.code() == 413) {
                                Toast.makeText(context, "1MB보다 큰 사진은 올릴 수 없어요", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "라벨 사진 업로드에 실패했어요", Toast.LENGTH_SHORT).show()
                            }
                            deleteCroppedImageFile(context, localImageUri)
                        }

                        yarnList.add(0, createdYarn)
                        showAddDialog = false
                        Toast.makeText(context, "새 실이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        deleteCroppedImageFile(context, localImageUri)
                        Toast.makeText(context, "네트워크 연결을 확인해 주세요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (yarnToEdit != null) {
        val target = yarnToEdit!!
        YarnFormDialog(
            initialYarn = target,
            titleText = "실 정보 수정",
            confirmText = "수정 완료",
            onDismiss = { yarnToEdit = null },
            onConfirm = { name, color, shop, purchaseDate, memo, newLocalImageUri, removeExistingImage ->
                coroutineScope.launch {
                    try {
                        val updateReq = YarnUpdateRequest(
                            name = name.trim().take(50),
                            color = color.trim().take(30).ifBlank { null },
                            purchaseSite = shop.trim().take(100).ifBlank { null },
                            purchaseDate = purchaseDate.trim().ifBlank { null },
                            memo = memo.trim().take(100).ifBlank { null }
                        )
                        val patchRes = resolvedApiService.updateYarn(target.id, updateReq)
                        if (!patchRes.isSuccessful || patchRes.body() == null) {
                            deleteCroppedImageFile(context, newLocalImageUri)
                            val msg = if (patchRes.code() == 422) "입력값 형식(날짜 YYYY-MM-DD 등)을 확인해 주세요"
                            else "실 정보 수정에 실패했어요 (${patchRes.code()})"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        var updatedYarn = patchRes.body()!!

                        val newLocalFile = uriStringToLocalFile(newLocalImageUri)
                        if (newLocalFile != null && newLocalFile.exists()) {
                            val part = createImageMultipart(newLocalFile)
                            val imgRes = resolvedApiService.uploadYarnImage(target.id, part)
                            if (imgRes.isSuccessful && imgRes.body() != null) {
                                // ImageUploadResponse(image_url, thumbnail_url)만 받아 기존 YarnData에 반영
                                val uploaded: ImageUploadResponse = imgRes.body()!!
                                updatedYarn = updatedYarn.copy(
                                    imageUrl = uploaded.imageUrl ?: updatedYarn.imageUrl,
                                    thumbnailUrl = uploaded.thumbnailUrl ?: updatedYarn.thumbnailUrl
                                )
                            } else if (imgRes.code() == 413) {
                                Toast.makeText(context, "1MB보다 큰 사진은 올릴 수 없어요", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "라벨 사진 변경에 실패했어요", Toast.LENGTH_SHORT).show()
                            }
                            deleteCroppedImageFile(context, newLocalImageUri)
                        } else if (removeExistingImage && target.imageUrl != null) {
                            val delImgRes = resolvedApiService.deleteYarnImage(target.id)
                            if (delImgRes.isSuccessful || delImgRes.code() == 204) {
                                updatedYarn = updatedYarn.copy(imageUrl = null, thumbnailUrl = null)
                            }
                        }

                        val idx = yarnList.indexOfFirst { it.id == updatedYarn.id }
                        if (idx != -1) {
                            yarnList[idx] = updatedYarn
                        }
                        if (selectedYarn?.id == updatedYarn.id) {
                            selectedYarn = updatedYarn
                        }
                        yarnToEdit = null
                        Toast.makeText(context, "실 정보가 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        deleteCroppedImageFile(context, newLocalImageUri)
                        Toast.makeText(context, "네트워크 연결을 확인해 주세요", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (yarnToDelete != null) {
        val target = yarnToDelete!!
        AlertDialog(
            onDismissRequest = { yarnToDelete = null },
            title = { Text("실 삭제", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${target.name}\" 실을 삭제하시겠습니까?\n\n이 실을 사용하는 카운터 프로젝트의 실 연결이 해제됩니다.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { performDeleteYarn(target) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("삭제", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { yarnToDelete = null }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }
}

private const val BASE_IMAGE_HOST = "https://api.hankovillage.com"

@Composable
private fun YarnImageBox(
    imageSource: Any?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallbackEmoji: String = "🧶",
    fallbackFontSize: Int = 32
) {
    val context = LocalContext.current
    var bitmap by remember(imageSource) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageSource) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                when (imageSource) {
                    is File -> {
                        if (imageSource.exists()) {
                            BitmapFactory.decodeFile(imageSource.absolutePath)?.asImageBitmap()
                        } else null
                    }
                    is String -> {
                        if (imageSource.isBlank()) null
                        else if (
                            imageSource.startsWith("http://") ||
                            imageSource.startsWith("https://") ||
                            imageSource.startsWith("/")
                        ) {
                            val fullUrl = if (imageSource.startsWith("/")) {
                                "$BASE_IMAGE_HOST$imageSource"
                            } else {
                                imageSource
                            }
                            val token = SessionManager.getAccessToken(context)
                            val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                                connectTimeout = 5000
                                readTimeout = 5000
                                if (!token.isNullOrBlank()) {
                                    setRequestProperty("Authorization", "Bearer $token")
                                }
                            }
                            conn.inputStream.use { stream ->
                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                            }
                        } else {
                            val parsed = Uri.parse(imageSource)
                            val localFile = if (parsed.scheme == "file" && parsed.path != null) {
                                File(parsed.path!!)
                            } else {
                                File(imageSource)
                            }
                            if (localFile.exists()) {
                                BitmapFactory.decodeFile(localFile.absolutePath)?.asImageBitmap()
                            } else {
                                context.contentResolver.openInputStream(parsed)?.use {
                                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                                }
                            }
                        }
                    }
                    else -> null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(fallbackEmoji, fontSize = fallbackFontSize.sp)
        }
    }
}
@Composable
private fun YarnCardItem(
    yarn: YarnData,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val displayImage = yarn.thumbnailUrl ?: yarn.imageUrl

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE2DDD0)),
                contentAlignment = Alignment.Center
            ) {
                YarnImageBox(
                    imageSource = displayImage,
                    contentDescription = yarn.name,
                    modifier = Modifier.fillMaxSize(),
                    fallbackEmoji = "🧶",
                    fallbackFontSize = 32
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = yarn.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = yarn.color?.takeIf { it.isNotBlank() } ?: "색상 미입력",
                fontSize = 12.sp,
                color = if (yarn.color.isNullOrBlank()) Color.Gray else PointOrange,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun YarnDetailContent(
    yarn: YarnData,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("목록으로", color = TextDark, fontWeight = FontWeight.Bold)
            }

            Row {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "수정", tint = TextDark)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg),
            contentAlignment = Alignment.Center
        ) {
            YarnImageBox(
                imageSource = yarn.imageUrl,
                contentDescription = yarn.name,
                modifier = Modifier.fillMaxSize(),
                fallbackEmoji = "🧶",
                fallbackFontSize = 56
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(yarn.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                DetailRow(label = "색상", value = yarn.color ?: "-")
                DetailRow(label = "구매처", value = yarn.shop ?: "-")
                DetailRow(label = "구매일", value = yarn.purchaseDate ?: "-")
                DetailRow(label = "메모", value = yarn.memo ?: "-")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.width(64.dp))
        Text(value.ifBlank { "-" }, fontSize = 13.sp, color = TextDark, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun YarnFormDialog(
    initialYarn: YarnData?,
    titleText: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        color: String,
        shop: String,
        purchaseDate: String,
        memo: String,
        newLocalImageUri: String?,
        removeExistingImage: Boolean
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialYarn?.name ?: "") }
    var color by remember { mutableStateOf(initialYarn?.color ?: "") }
    var shop by remember { mutableStateOf(initialYarn?.shop ?: "") }
    var purchaseDate by remember { mutableStateOf(initialYarn?.purchaseDate ?: "") }
    var memo by remember { mutableStateOf(initialYarn?.memo ?: "") }

    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCroppedUri by remember { mutableStateOf<String?>(null) }
    var removeExistingImage by remember { mutableStateOf(false) }

    val handleDismiss = {
        deleteCroppedImageFile(context, selectedCroppedUri)
        onDismiss()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCropUri = uri
        }
    }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onDismiss = { pendingCropUri = null },
            onCropComplete = { croppedUri ->
                deleteCroppedImageFile(context, selectedCroppedUri)
                selectedCroppedUri = croppedUri.toString()
                removeExistingImage = false
                pendingCropUri = null
            }
        )
    }

    val existingUrl = initialYarn?.imageUrl
    val previewModel: Any? = when {
        selectedCroppedUri != null -> selectedCroppedUri
        !removeExistingImage && !existingUrl.isNullOrBlank() -> existingUrl
        else -> null
    }

    val isDateValid = remember(purchaseDate) { isValidPurchaseDate(purchaseDate) }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text(titleText, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CardBg)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewModel != null) {
                            YarnImageBox(
                                imageSource = previewModel,
                                contentDescription = "라벨 사진",
                                modifier = Modifier.fillMaxSize(),
                                fallbackEmoji = "🧶",
                                fallbackFontSize = 24
                            )
                        } else {
                            Text("+ 사진", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("라벨 사진 선택", color = PointOrange, fontSize = 13.sp)
                        }
                        if (previewModel != null) {
                            TextButton(
                                onClick = {
                                    deleteCroppedImageFile(context, selectedCroppedUri)
                                    selectedCroppedUri = null
                                    removeExistingImage = true
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("사진 제거", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("실 이름 (필수)") },
                    supportingText = { Text("${name.length}/50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = color,
                    onValueChange = { if (it.length <= 30) color = it },
                    label = { Text("색상 (선택)") },
                    supportingText = { Text("${color.length}/30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = shop,
                    onValueChange = { if (it.length <= 100) shop = it },
                    label = { Text("구매처 (선택)") },
                    supportingText = { Text("${shop.length}/100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purchaseDate,
                    onValueChange = { if (it.length <= 10) purchaseDate = it },
                    label = { Text("구매일 (YYYY-MM-DD, 선택)") },
                    supportingText = {
                        if (!isDateValid) {
                            Text("YYYY-MM-DD 형식의 올바른 날짜를 입력해 주세요", color = Color.Red)
                        } else {
                            Text("예: 2026-11-23")
                        }
                    },
                    isError = !isDateValid,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memo,
                    onValueChange = { if (it.length <= 100) memo = it },
                    label = { Text("메모 (선택)") },
                    supportingText = { Text("${memo.length}/100") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && isDateValid) {
                        onConfirm(
                            name,
                            color,
                            shop,
                            purchaseDate,
                            memo,
                            selectedCroppedUri,
                            removeExistingImage
                        )
                    }
                },
                enabled = name.isNotBlank() && isDateValid,
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text(confirmText, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismiss) {
                Text("취소", color = Color.Gray)
            }
        }
    )
}

private fun isValidPurchaseDate(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return true
    if (!Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(trimmed)) return false

    return try {
        val parts = trimmed.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        if (month !in 1..12 || day !in 1..31) return false

        val cal = Calendar.getInstance().apply {
            isLenient = false
            set(year, month - 1, day)
        }
        cal.time
        true
    } catch (_: Exception) {
        false
    }
}

private fun uriStringToLocalFile(uriString: String?): File? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val parsed = Uri.parse(uriString)
        if (parsed.scheme == "file" && parsed.path != null) {
            File(parsed.path!!)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

private fun createImageMultipart(file: File): MultipartBody.Part {
    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData("file", file.name, requestBody)
}