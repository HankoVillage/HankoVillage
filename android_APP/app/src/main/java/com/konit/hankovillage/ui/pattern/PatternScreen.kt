package com.konit.hankovillage.ui.pattern

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konit.hankovillage.network.HankoApiService
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SessionManager
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark
import com.konit.hankovillage.ui.util.ImageCropDialog
import com.konit.hankovillage.ui.util.decodeSampledBitmapFromUri
import com.konit.hankovillage.ui.util.deleteCroppedImageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

val AvailableTags = listOf("의류", "목도리", "모자", "가방", "인형", "기타")
private const val BASE_IMAGE_HOST = "https://api.hankovillage.com"

@Composable
fun PatternImage(
    imageSource: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    if (imageSource.startsWith("file://") || imageSource.startsWith("content://")) {
        val bitmap = remember(imageSource) {
            decodeSampledBitmapFromUri(
                context = context,
                uri = Uri.parse(imageSource),
                reqWidth = 800,
                reqHeight = 800
            )
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            ImagePlaceholder(modifier)
        }
    } else {
        val fullUrl = if (imageSource.startsWith("http")) imageSource else "$BASE_IMAGE_HOST$imageSource"
        var remoteBitmap by remember(fullUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }

        LaunchedEffect(fullUrl) {
            remoteBitmap = withContext(Dispatchers.IO) {
                try {
                    val token = SessionManager.getAccessToken(context)
                    val client = OkHttpClient()
                    val requestBuilder = Request.Builder().url(fullUrl)
                    if (!token.isNullOrBlank()) {
                        requestBuilder.addHeader("Authorization", "Bearer $token")
                    }
                    client.newCall(requestBuilder.build()).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } else null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        if (remoteBitmap != null) {
            Image(
                bitmap = remoteBitmap!!.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        } else {
            ImagePlaceholder(modifier)
        }
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Text("--", fontSize = 20.sp, color = Color.DarkGray)
    }
}

private fun uriToMultipartPart(uriString: String): MultipartBody.Part? {
    return try {
        val uri = Uri.parse(uriString)
        val path = uri.path ?: return null
        val file = File(path)
        if (!file.exists()) return null
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        MultipartBody.Part.createFormData("file", file.name, requestBody)
    } catch (_: Exception) {
        null
    }
}

private fun validateAndFormatUrl(context: Context, rawUrl: String): Pair<Boolean, String?> {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return true to null
    if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        Toast.makeText(context, "도안 링크는 http:// 또는 https://로 시작해야 해요.", Toast.LENGTH_SHORT).show()
        return false to null
    }
    return true to trimmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternScreen(
    patternList: SnapshotStateList<PatternData> = remember { mutableStateListOf() },
    isLoggedIn: Boolean = true,
    apiService: HankoApiService? = null,
    onAddPattern: (PatternData) -> Unit = { newPattern -> patternList.add(0, newPattern) },
    onUpdatePattern: (PatternData) -> Unit = { updated ->
        val index = patternList.indexOfFirst { it.id == updated.id }
        if (index != -1) patternList[index] = updated
    },
    onDeletePattern: (Long) -> Unit = { deletedId ->
        patternList.removeIf { it.id == deletedId }
    }
) {
    val context = LocalContext.current
    val resolvedApiService = remember(apiService, context) {
        apiService ?: RetrofitInstance.getApiService(context)
    }
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf<PatternStatus?>(null) }
    var selectedSortOrder by rememberSaveable { mutableStateOf(PatternSortOrder.NEWEST) }

    var selectedPattern by remember { mutableStateOf<PatternData?>(null) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            try {
                val response = resolvedApiService.getPatterns(limit = 100)
                if (response.isSuccessful && response.body() != null) {
                    patternList.clear()
                    patternList.addAll(response.body()!!)
                }
            } catch (_: Exception) {
            }
        }
    }

    val trimmedQuery = searchQuery.trim()
    val filteredPatterns = patternList.filter { pattern ->
        val matchesQuery = if (trimmedQuery.length >= 2) {
            pattern.title.contains(trimmedQuery, ignoreCase = true) ||
                    pattern.tag.equals(trimmedQuery, ignoreCase = true) ||
                    pattern.tagCode.equals(trimmedQuery, ignoreCase = true)
        } else true

        val matchesStatus = selectedStatusFilter?.let { pattern.status == it } ?: true
        matchesQuery && matchesStatus
    }.sortedWith { p1, p2 ->
        if (selectedSortOrder == PatternSortOrder.NEWEST) p2.id.compareTo(p1.id)
        else p1.title.compareTo(p2.title)
    }

    if (selectedPattern != null) {
        BackHandler {
            selectedPattern = null
        }

        PatternDetailContent(
            pattern = selectedPattern!!,
            isLoggedIn = isLoggedIn,
            apiService = resolvedApiService,
            onBackClick = { selectedPattern = null },
            onUpdate = { updated ->
                onUpdatePattern(updated)
                selectedPattern = updated
            },
            onDelete = { deletedId ->
                onDeletePattern(deletedId)
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
                placeholder = { Text("제목, 태그 검색 (2글자 이상)", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = if (trimmedQuery.length == 1) Color.Gray else PointOrange
                    )
                },
                supportingText = if (trimmedQuery.length == 1) {
                    { Text("검색어는 2글자 이상 입력해 주세요.", fontSize = 11.sp, color = Color.Gray) }
                } else null,
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
                                selectedStatusFilter =
                                    if (selectedStatusFilter == status) null else status
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
                    Text(
                        text = "정렬: ${selectedSortOrder.label}",
                        fontSize = 12.sp,
                        color = PointOrange
                    )
                }
            }

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
            onConfirm = { draftPattern, localImageUri ->
                if (isLoggedIn) {
                    coroutineScope.launch {
                        try {
                            val createRes = resolvedApiService.createPattern(
                                PatternCreateRequest(
                                    title = draftPattern.title,
                                    tag = draftPattern.tagCode,
                                    status = draftPattern.statusCode,
                                    linkUrl = draftPattern.patternUrl,
                                    yarnInfo = draftPattern.yarnInfo
                                )
                            )
                            if (createRes.isSuccessful && createRes.body() != null) {
                                var created = createRes.body()!!

                                if (!localImageUri.isNullOrBlank()) {
                                    val part = uriToMultipartPart(localImageUri)
                                    if (part != null) {
                                        val imgRes = resolvedApiService.uploadPatternImage(created.id, part)
                                        if (imgRes.isSuccessful && imgRes.body() != null) {
                                            val uploaded = imgRes.body()!!
                                            created = created.copy(
                                                imageUrl = uploaded.imageUrl ?: created.imageUrl,
                                                thumbnailUrl = uploaded.thumbnailUrl ?: created.thumbnailUrl
                                            )
                                        }
                                    }
                                    deleteCroppedImageFile(context, localImageUri)
                                }

                                onAddPattern(created)
                                showUploadDialog = false
                            } else {
                                Toast.makeText(context, "등록 실패 (${createRes.code()})", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    onAddPattern(draftPattern.copy(id = System.currentTimeMillis(), imageUrl = localImageUri))
                    showUploadDialog = false
                }
            }
        )
    }
}

@Composable
fun PatternCardItem(
    pattern: PatternData,
    onClick: () -> Unit
) {
    val displayImage = pattern.thumbnailUrl ?: pattern.imageUrl

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

                Text(
                    text = "#${pattern.tag}",
                    fontSize = 11.sp,
                    color = PointOrange,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!displayImage.isNullOrEmpty()) {
                PatternImage(
                    imageSource = displayImage,
                    contentDescription = pattern.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = pattern.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (!pattern.yarnInfo.isNullOrBlank()) {
                Text(
                    text = "사용 실: ${pattern.yarnInfo}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PatternDetailContent(
    pattern: PatternData,
    isLoggedIn: Boolean = true,
    apiService: HankoApiService? = null,
    onBackClick: () -> Unit,
    onUpdate: (PatternData) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val resolvedApiService = remember(apiService, context) {
        apiService ?: RetrofitInstance.getApiService(context)
    }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val detailGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingCropUri = it }
    }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onDismiss = { pendingCropUri = null },
            onCropComplete = { croppedUri ->
                val localUriStr = croppedUri.toString()
                pendingCropUri = null

                if (isLoggedIn) {
                    coroutineScope.launch {
                        try {
                            val part = uriToMultipartPart(localUriStr)
                            if (part != null) {
                                val res = resolvedApiService.uploadPatternImage(pattern.id, part)
                                if (res.isSuccessful && res.body() != null) {
                                    val uploaded = res.body()!!
                                    onUpdate(
                                        pattern.copy(
                                            imageUrl = uploaded.imageUrl ?: pattern.imageUrl,
                                            thumbnailUrl = uploaded.thumbnailUrl ?: pattern.thumbnailUrl
                                        )
                                    )
                                    Toast.makeText(context, "사진이 등록되었습니다.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "사진 업로드 실패 (${res.code()})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "사진 업로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            deleteCroppedImageFile(context, localUriStr)
                        }
                    }
                } else {
                    deleteCroppedImageFile(context, pattern.imageUrl)
                    onUpdate(pattern.copy(imageUrl = localUriStr))
                }
            }
        )
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
                Text("< 도안 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
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
                .height(if (!pattern.imageUrl.isNullOrEmpty()) 200.dp else 100.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBg)
        ) {
            if (!pattern.imageUrl.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    PatternImage(
                        imageSource = pattern.imageUrl!!,
                        contentDescription = pattern.title,
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
                        text = "완성작 / 도안 사진 등록하기",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                        if (isSelected) return@FilterChip
                        if (isLoggedIn) {
                            coroutineScope.launch {
                                try {
                                    val res = resolvedApiService.updatePattern(
                                        pattern.id,
                                        PatternUpdateRequest(status = statusOption.serverCode)
                                    )
                                    if (res.isSuccessful && res.body() != null) {
                                        onUpdate(res.body()!!)
                                    } else {
                                        Toast.makeText(context, "상태 변경 실패 (${res.code()})", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            onUpdate(pattern.copy(statusCode = statusOption.serverCode))
                        }
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
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("사용 실: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text(
                    text = pattern.yarnInfo?.takeIf { it.isNotBlank() } ?: "미입력",
                    fontSize = 14.sp,
                    color = TextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!pattern.patternUrl.isNullOrBlank()) {
            Button(
                onClick = {
                    try {
                        val rawUrl = pattern.patternUrl!!.trim()
                        val formattedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                            "https://$rawUrl"
                        } else {
                            rawUrl
                        }

                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "링크를 열 수 없어요. 유효한 웹주소인지 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("도안 링크 바로가기", color = Color.White, fontWeight = FontWeight.Bold)
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
            onConfirm = { updatedData, newLocalImageUri ->
                if (isLoggedIn) {
                    coroutineScope.launch {
                        try {
                            val patchRes = resolvedApiService.updatePattern(
                                pattern.id,
                                PatternUpdateRequest(
                                    title = updatedData.title,
                                    tag = updatedData.tagCode,
                                    status = updatedData.statusCode,
                                    linkUrl = updatedData.patternUrl,
                                    yarnInfo = updatedData.yarnInfo
                                )
                            )
                            if (patchRes.isSuccessful && patchRes.body() != null) {
                                var latest = patchRes.body()!!

                                if (newLocalImageUri != null && newLocalImageUri.startsWith("file://")) {
                                    val part = uriToMultipartPart(newLocalImageUri)
                                    if (part != null) {
                                        val imgRes = resolvedApiService.uploadPatternImage(pattern.id, part)
                                        if (imgRes.isSuccessful && imgRes.body() != null) {
                                            val uploaded = imgRes.body()!!
                                            latest = latest.copy(
                                                imageUrl = uploaded.imageUrl ?: latest.imageUrl,
                                                thumbnailUrl = uploaded.thumbnailUrl ?: latest.thumbnailUrl
                                            )
                                        }
                                    }
                                    deleteCroppedImageFile(context, newLocalImageUri)
                                }

                                onUpdate(latest)
                                showEditDialog = false
                            } else {
                                Toast.makeText(context, "수정 실패 (${patchRes.code()})", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (newLocalImageUri != pattern.imageUrl) {
                        deleteCroppedImageFile(context, pattern.imageUrl)
                    }
                    onUpdate(updatedData.copy(imageUrl = newLocalImageUri))
                    showEditDialog = false
                }
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
                    onClick = {
                        if (isLoggedIn) {
                            coroutineScope.launch {
                                try {
                                    val res = resolvedApiService.deletePattern(pattern.id)
                                    if (res.isSuccessful) {
                                        deleteCroppedImageFile(context, pattern.imageUrl)
                                        onDelete(pattern.id)
                                    } else {
                                        Toast.makeText(context, "삭제 실패 (${res.code()})", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            deleteCroppedImageFile(context, pattern.imageUrl)
                            onDelete(pattern.id)
                        }
                    },
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

@Composable
fun PatternUploadDialog(
    onDismiss: () -> Unit,
    onConfirm: (PatternData, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf(AvailableTags.first()) }
    var yarnInfo by remember { mutableStateOf("") }
    var patternUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingCropUri = it }
    }

    val handleDismiss = {
        deleteCroppedImageFile(context, imageUri)
        onDismiss()
    }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onDismiss = { pendingCropUri = null },
            onCropComplete = { croppedUri ->
                deleteCroppedImageFile(context, imageUri)
                imageUri = croppedUri.toString()
                pendingCropUri = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text("새 도안 등록", fontWeight = FontWeight.Bold) },
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
                        PatternImage(
                            imageSource = imageUri!!,
                            contentDescription = "선택한 도안 사진",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PointOrange)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("도안 / 완성작 사진 추가 (선택)", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("도안 제목 (최대 50자) *") },
                    supportingText = {
                        Text(
                            text = "${title.length}/50자",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("태그 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                    onValueChange = { if (it.length <= 50) yarnInfo = it },
                    label = { Text("사용 실 (최대 50자)") },
                    supportingText = {
                        Text(
                            text = "${yarnInfo.length}/50자",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    },
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
                        val (isValidUrl, validatedLink) = validateAndFormatUrl(context, patternUrl)
                        if (!isValidUrl) return@Button

                        onConfirm(
                            PatternData(
                                title = title.trim(),
                                tagCode = PatternData.toServerTag(selectedTag),
                                yarnInfo = yarnInfo.trim().ifBlank { null },
                                patternUrl = validatedLink
                            ),
                            imageUri
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("등록", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismiss) { Text("취소", color = Color.Gray) }
        }
    )
}

@Composable
fun PatternEditDialog(
    pattern: PatternData,
    onDismiss: () -> Unit,
    onConfirm: (PatternData, String?) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(pattern.title) }
    var selectedTag by remember { mutableStateOf(pattern.tag) }
    var yarnInfo by remember { mutableStateOf(pattern.yarnInfo ?: "") }
    var patternUrl by remember { mutableStateOf(pattern.patternUrl ?: "") }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingCropUri = it }
    }

    val initialImageUri = pattern.imageUrl
    var imageUri by remember { mutableStateOf(initialImageUri) }

    val handleDismiss = {
        if (imageUri != initialImageUri) {
            deleteCroppedImageFile(context, imageUri)
        }
        onDismiss()
    }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onDismiss = { pendingCropUri = null },
            onCropComplete = { croppedUri ->
                if (imageUri != initialImageUri) {
                    deleteCroppedImageFile(context, imageUri)
                }
                imageUri = croppedUri.toString()
                pendingCropUri = null
            }
        )
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        title = { Text("도안 정보 수정", fontWeight = FontWeight.Bold) },
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
                        PatternImage(
                            imageSource = imageUri!!,
                            contentDescription = "수정된 도안 사진",
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
                    value = title,
                    onValueChange = { if (it.length <= 50) title = it },
                    label = { Text("도안 제목 (최대 50자)") },
                    supportingText = {
                        Text(
                            text = "${title.length}/50자",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("태그 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                    onValueChange = { if (it.length <= 50) yarnInfo = it },
                    label = { Text("사용 실 (최대 50자)") },
                    supportingText = {
                        Text(
                            text = "${yarnInfo.length}/50자",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    },
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
                        val (isValidUrl, validatedLink) = validateAndFormatUrl(context, patternUrl)
                        if (!isValidUrl) return@Button

                        if (imageUri != initialImageUri) {
                            deleteCroppedImageFile(context, initialImageUri)
                        }

                        onConfirm(
                            pattern.copy(
                                title = title.trim(),
                                tagCode = PatternData.toServerTag(selectedTag),
                                yarnInfo = yarnInfo.trim().ifBlank { null },
                                patternUrl = validatedLink
                            ),
                            if (imageUri != initialImageUri) imageUri else null
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("수정 완료", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismiss) { Text("취소", color = Color.Gray) }
        }
    )
}