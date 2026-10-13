package com.konit.hankovillage.ui.community

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

/**
 * 커뮤니티 메인 화면 컴포저블
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    postList: SnapshotStateList<PostData> = remember { mutableStateListOf() },
    isLoggedIn: Boolean = true,
    currentNickname: String = "김뜨개",
    onRequireLogin: () -> Unit = {}
) {
    // 초기 더미 데이터 설정 (리스트가 비어있을 때 최초 1회만 초기화)
    LaunchedEffect(Unit) {
        if (postList.isEmpty()) {
            postList.add(
                PostData(
                    id = 1L,
                    title = "이번에 완성한 대바늘 꽈배기 니트 뽐냅니다!",
                    category = CommunityCategory.SHOWOFF,
                    authorName = currentNickname,
                    createdAt = "2026-10-10",
                    content = "3주 동안 도안 보면서 열공해서 만들었어요! 생각보다 핏이 너무 예쁘게 나와서 만족스럽습니다.",
                    likeCount = 12,
                    comments = mutableListOf(
                        CommentData(101L, "뜨개장인", "와 너무 예뻐요! 어떤 실 쓰셨나요?", "2026-10-10 14:20"),
                        CommentData(102L, currentNickname, "메리노울 100g 4.0mm 바늘 썼습니다!", "2026-10-10 14:35", isMyComment = true)
                    ),
                    isMyPost = true
                )
            )
        }
    }

    var selectedCategory by remember { mutableStateOf(CommunityCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedPost by remember { mutableStateOf<PostData?>(null) }
    var isEditingPost by remember { mutableStateOf(false) }
    var isCreatingPost by remember { mutableStateOf(false) }

    var showAuthDialog by remember { mutableStateOf(false) }

    val baseList = if (!isLoggedIn) postList.take(5) else postList

    val filteredPosts = baseList.filter { post ->
        val matchesCategory = if (selectedCategory == CommunityCategory.ALL) true else post.category == selectedCategory
        val matchesQuery = if (searchQuery.trim().length >= 2) {
            post.title.contains(searchQuery, ignoreCase = true) || post.content.contains(searchQuery, ignoreCase = true)
        } else true
        matchesCategory && matchesQuery
    }

    if (selectedPost != null) {
        BackHandler {
            selectedPost = null
            isEditingPost = false
        }

        if (isEditingPost) {
            PostFormContent(
                initialPost = selectedPost,
                currentNickname = currentNickname,
                onBack = { isEditingPost = false },
                onSave = { updated ->
                    val idx = postList.indexOfFirst { it.id == updated.id }
                    if (idx != -1) postList[idx] = updated
                    selectedPost = updated
                    isEditingPost = false
                }
            )
        } else {
            PostDetailContent(
                post = selectedPost!!,
                isLoggedIn = isLoggedIn,
                currentNickname = currentNickname,
                onBack = { selectedPost = null },
                onEdit = { isEditingPost = true },
                onDelete = { deletedId ->
                    postList.removeIf { it.id == deletedId }
                    selectedPost = null
                },
                onRequireAuth = { showAuthDialog = true }
            )
        }
    } else if (isCreatingPost) {
        BackHandler { isCreatingPost = false }

        PostFormContent(
            initialPost = null,
            currentNickname = currentNickname,
            onBack = { isCreatingPost = false },
            onSave = { newPost ->
                postList.add(0, newPost)
                isCreatingPost = false
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
                Text("커뮤니티", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)

                FloatingActionButton(
                    onClick = {
                        if (isLoggedIn) isCreatingPost = true else showAuthDialog = true
                    },
                    containerColor = PointOrange,
                    contentColor = Color.White,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "글쓰기")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    if (!isLoggedIn) showAuthDialog = true else searchQuery = it
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("제목 또는 내용 검색 (2글자 이상)", fontSize = 13.sp) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CommunityCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            if (!isLoggedIn && cat != CommunityCategory.ALL) {
                                showAuthDialog = true
                            } else {
                                selectedCategory = cat
                            }
                        },
                        label = { Text(cat.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PointOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredPosts, key = { it.id }) { post ->
                    PostCardItem(
                        post = post,
                        onClick = { selectedPost = post }
                    )
                }
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("로그인 필요", fontWeight = FontWeight.Bold) },
            text = { Text("커뮤니티 글쓰기, 검색, 댓글 및 좋아요 기능은 회원전용 서비스입니다. 로그인하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        showAuthDialog = false
                        onRequireLogin()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("로그인하러 가기", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}

/**
 * 게시글 항목 카드 컴포저블
 */
@Composable
fun PostCardItem(
    post: PostData,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = PointOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = post.category.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = post.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${post.authorName} · ${post.createdAt}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("좋아요 ${post.likeCount}", fontSize = 11.sp, color = Color.Gray)
                    Text("댓글 ${post.comments.size}", fontSize = 11.sp, color = Color.Gray)
                }
            }

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0xFFDCD7C9), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("사진", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * 게시글 상세 내용 및 댓글 영역 컴포저블
 */
@Composable
fun PostDetailContent(
    post: PostData,
    isLoggedIn: Boolean,
    currentNickname: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: (Long) -> Unit,
    onRequireAuth: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newCommentText by remember { mutableStateOf("") }
    var editingCommentId by remember { mutableStateOf<Long?>(null) }
    var commentToEditContent by remember { mutableStateOf("") }
    var commentToDeleteId by remember { mutableStateOf<Long?>(null) }

    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }
    var likeCount by remember(post.id) { mutableIntStateOf(post.likeCount) }

    val commentsList = remember(post.id) { post.comments.toMutableStateList() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
            TextButton(onClick = onBack) {
                Text("< 목록으로", color = PointOrange, fontWeight = FontWeight.Bold)
            }

            Row {
                // 💡 [수정] 로그인 상태이면서 작성 본인일 때만 수정/삭제 버튼 노출
                if (isLoggedIn && post.isMyPost) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = TextDark)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Red)
                    }
                } else {
                    IconButton(onClick = {
                        if (isLoggedIn) showReportDialog = true else onRequireAuth()
                    }) {
                        Icon(Icons.Default.Warning, contentDescription = "신고", tint = Color.Gray)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    color = PointOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = post.category.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(post.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text("${post.authorName} · ${post.createdAt}", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFDCD7C9), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("게시글 본문 사진", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val displayContent = if (!isLoggedIn && post.content.length > 100) {
                    post.content.take(100) + "...\n\n나머지 본문과 댓글은 로그인 후 볼 수 있습니다."
                } else post.content

                Text(displayContent, fontSize = 14.sp, color = TextDark, lineHeight = 20.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        if (!isLoggedIn) {
                            onRequireAuth()
                        } else {
                            isLiked = !isLiked
                            if (isLiked) {
                                likeCount++
                                post.isLiked = true
                                post.likeCount++
                            } else {
                                likeCount--
                                post.isLiked = false
                                post.likeCount--
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "좋아요",
                            tint = if (isLiked) Color.Red else Color.Gray
                        )
                    }
                    Text("좋아요 ${likeCount}개", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                }

                HorizontalDivider(color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                Text("댓글 목록 (${commentsList.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }

            if (!isLoggedIn) {
                item {
                    Text("로그인 후 댓글을 확인해 보세요.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                items(commentsList, key = { it.id }) { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(comment.authorName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(comment.createdAt, fontSize = 10.sp, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (editingCommentId == comment.id) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = commentToEditContent,
                                        onValueChange = { commentToEditContent = it },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    TextButton(onClick = {
                                        val index = commentsList.indexOfFirst { it.id == comment.id }
                                        if (index != -1) {
                                            commentsList[index] = commentsList[index].copy(content = commentToEditContent)
                                            post.comments[index].content = commentToEditContent
                                        }
                                        editingCommentId = null
                                    }) { Text("저장") }
                                }
                            } else {
                                Text(comment.content, fontSize = 13.sp, color = TextDark)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (comment.isMyComment) {
                                        Text(
                                            "수정",
                                            fontSize = 11.sp,
                                            color = PointOrange,
                                            modifier = Modifier.clickable {
                                                editingCommentId = comment.id
                                                commentToEditContent = comment.content
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "삭제",
                                            fontSize = 11.sp,
                                            color = Color.Red,
                                            modifier = Modifier.clickable { commentToDeleteId = comment.id }
                                        )
                                    } else {
                                        Text(
                                            "신고",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.clickable { showReportDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isLoggedIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = { Text("댓글을 입력하세요...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newCommentText.isNotBlank()) {
                            val newComment = CommentData(
                                id = System.currentTimeMillis(),
                                authorName = currentNickname,
                                content = newCommentText,
                                createdAt = "방금 전",
                                isMyComment = true
                            )
                            commentsList.add(newComment)
                            post.comments.add(newComment)
                            newCommentText = ""

                            coroutineScope.launch {
                                listState.animateScrollToItem(commentsList.size)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("등록", color = Color.White)
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(onDismiss = { showReportDialog = false })
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("게시글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("게시글을 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = { onDelete(post.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("삭제", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            }
        )
    }

    if (commentToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { commentToDeleteId = null },
            title = { Text("댓글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("댓글을 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        commentsList.removeIf { it.id == commentToDeleteId }
                        post.comments.removeIf { it.id == commentToDeleteId }
                        commentToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("삭제", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { commentToDeleteId = null }) { Text("취소") }
            }
        )
    }
}

/**
 * 게시글 작성 및 수정 입력 양식 컴포저블
 */
@Composable
fun PostFormContent(
    initialPost: PostData?,
    currentNickname: String,
    onBack: () -> Unit,
    onSave: (PostData) -> Unit
) {
    var title by remember { mutableStateOf(initialPost?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(initialPost?.category ?: CommunityCategory.SHOWOFF) }
    var content by remember { mutableStateOf(initialPost?.content ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("< 취소", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val result = initialPost?.copy(
                            title = title,
                            category = selectedCategory,
                            content = content
                        ) ?: PostData(
                            id = System.currentTimeMillis(),
                            title = title,
                            category = selectedCategory,
                            authorName = currentNickname,
                            createdAt = "방금 전",
                            content = content,
                            isMyPost = true
                        )
                        onSave(result)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text(if (initialPost == null) "등록" else "수정 저장", color = Color.White)
            }
        }

        Text("카테고리 선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CommunityCategory.entries.filter { it != CommunityCategory.ALL }.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.label, fontSize = 12.sp) }
                )
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("대표 사진 1장 선택/촬영")
        }

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("내용") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

/**
 * 게시글/댓글 신고 사유 선택 다이얼로그
 */
@Composable
fun ReportDialog(onDismiss: () -> Unit) {
    val reportReasons = listOf("불쾌한 내용 / 욕설", "부적절한 홍보 / 스팸", "도용 / 저작권 침해", "기타")
    var selectedReason by remember { mutableStateOf(reportReasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("신고 사유 선택", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                reportReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason }
                        )
                        Text(reason, fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
            ) {
                Text("신고 제출", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        }
    )
}