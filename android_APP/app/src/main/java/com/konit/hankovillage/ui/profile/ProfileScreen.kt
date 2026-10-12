package com.konit.hankovillage.ui.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgColor = Color(0xFFF7F4EB)
val CardBg = Color(0xFFEFECE1)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

enum class ProfileSubScreen {
    NONE, EDIT_PROFILE, MY_POSTS, LIKED_POSTS, NOTIFICATION_SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfileData = UserProfileData(),
    onUpdateProfile: (UserProfileData) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current

    // 전달받은 userProfile 상태 동적 관리
    var profile by remember(userProfile) { mutableStateOf(userProfile) }

    // 완성작 갤러리 더미 데이터
    val completedWorks = remember {
        listOf(
            CompletedWork(1L, "가을 체크 머플러"),
            CompletedWork(2L, "베이지 그래니 스퀘어 가방"),
            CompletedWork(3L, "파스텔 코튼 수세미")
        )
    }

    // 작성한 글 / 좋아요한 글 더미 데이터
    val myPosts = remember {
        mutableStateListOf(
            "이번에 완성한 대바늘 꽈배기 니트 뽐냅니다! 🧶",
            "동대문 종합시장 실 쇼핑 후기"
        )
    }
    val likedPosts = remember {
        mutableStateListOf(
            "탑다운 코줍기 구멍 안 생기는 꿀팁 공유",
            "초보자를 위한 코바늘 기호 모음집"
        )
    }

    var currentSubScreen by remember { mutableStateOf(ProfileSubScreen.NONE) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (currentSubScreen != ProfileSubScreen.NONE) {
        BackHandler { currentSubScreen = ProfileSubScreen.NONE }
    }

    when (currentSubScreen) {
        ProfileSubScreen.EDIT_PROFILE -> {
            // ✏️ 프로필 수정 화면
            EditProfileContent(
                profile = profile,
                onBack = { currentSubScreen = ProfileSubScreen.NONE },
                onSave = { updatedProfile ->
                    profile = updatedProfile
                    onUpdateProfile(updatedProfile)
                    Toast.makeText(context, "프로필이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    currentSubScreen = ProfileSubScreen.NONE
                }
            )
        }
        ProfileSubScreen.MY_POSTS -> {
            // 📝 작성한 게시글 화면
            SimplePostListContent(
                title = "작성한 게시글",
                posts = myPosts,
                onBack = { currentSubScreen = ProfileSubScreen.NONE }
            )
        }
        ProfileSubScreen.LIKED_POSTS -> {
            // ❤️ 좋아요한 게시글 화면
            SimplePostListContent(
                title = "좋아요한 게시글",
                posts = likedPosts,
                onBack = { currentSubScreen = ProfileSubScreen.NONE }
            )
        }
        ProfileSubScreen.NOTIFICATION_SETTINGS -> {
            // 🔔 알림 설정 화면
            NotificationSettingsContent(
                profile = profile,
                onBack = { currentSubScreen = ProfileSubScreen.NONE },
                onToggleComment = { isChecked ->
                    val updated = profile.copy(commentNotification = isChecked)
                    profile = updated
                    onUpdateProfile(updated)
                },
                onToggleLike = { isChecked ->
                    val updated = profile.copy(likeNotification = isChecked)
                    profile = updated
                    onUpdateProfile(updated)
                }
            )
        }
        ProfileSubScreen.NONE -> {
            // 👤 내 프로필 메인 화면
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "내 프로필 👤",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 프로필 카드 영역
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 프로필 사진
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCD7C9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "프로필 사진",
                                modifier = Modifier.size(50.dp),
                                tint = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = profile.nickname,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = profile.bio,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { currentSubScreen = ProfileSubScreen.EDIT_PROFILE },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PointOrange),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PointOrange),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("프로필 수정", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 🏆 완성작 갤러리
                Text(
                    text = "완성작 갤러리 🏆 (${completedWorks.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (completedWorks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(CardBg, shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("아직 완성된 도안이 없어요!", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        completedWorks.forEach { work ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFDCD7C9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🖼️", fontSize = 20.sp)
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = work.title,
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ⚙️ 메뉴 리스트
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Edit,
                            title = "작성한 글",
                            onClick = { currentSubScreen = ProfileSubScreen.MY_POSTS }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "좋아요한 글",
                            onClick = { currentSubScreen = ProfileSubScreen.LIKED_POSTS }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "알림 설정",
                            onClick = { currentSubScreen = ProfileSubScreen.NOTIFICATION_SETTINGS }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.ExitToApp,
                            title = "로그아웃",
                            onClick = { showLogoutDialog = true }
                        )
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Delete,
                            title = "회원 탈퇴",
                            titleColor = Color.Red,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }
        }
    }

    // 🚪 로그아웃 확인 팝업
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("로그아웃", fontWeight = FontWeight.Bold) },
            text = { Text("로그아웃할까요?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PointOrange)
                ) {
                    Text("로그아웃", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    // ⚠️ 회원 탈퇴 안내 및 동의 팝업
    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteAccountDialog = false },
            onConfirmDelete = {
                showDeleteAccountDialog = false
                Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        )
    }
}

// 📌 메뉴 항목 컴포저블
@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    titleColor: Color = TextDark,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, fontSize = 14.sp, color = titleColor, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

// ✏️ 프로필 수정 화면
@Composable
fun EditProfileContent(
    profile: UserProfileData,
    onBack: () -> Unit,
    onSave: (UserProfileData) -> Unit
) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf(profile.nickname) }
    var bio by remember { mutableStateOf(profile.bio) }
    var isCheckedNickname by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    if (isCheckedNickname && nickname.isNotBlank()) {
                        onSave(profile.copy(nickname = nickname, bio = bio))
                    } else {
                        Toast.makeText(context, "닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                enabled = nickname.isNotBlank()
            ) {
                Text("저장", color = Color.White)
            }
        }

        Text("프로필 수정 ✏️", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)

        // 사진 수정
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(Color(0xFFDCD7C9))
                .clickable { /* 사진 선택/촬영 */ },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PointOrange)
                Text("사진 변경", fontSize = 10.sp, color = Color.Gray)
            }
        }

        // 닉네임 수정 및 중복 확인
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    isCheckedNickname = false
                },
                label = { Text("닉네임 *") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    isCheckedNickname = true
                    Toast.makeText(context, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text("중복 확인", color = TextDark, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("소개글") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
    }
}

// 📝/❤️ 게시글 목록 간단 조회 화면
@Composable
fun SimplePostListContent(
    title: String,
    posts: List<String>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (posts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("목록이 없습니다.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(posts) { postTitle ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* 게시글 상세 이동 */ },
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = postTitle,
                            modifier = Modifier.padding(14.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                    }
                }
            }
        }
    }
}

// 🔔 알림 설정 화면
@Composable
fun NotificationSettingsContent(
    profile: UserProfileData,
    onBack: () -> Unit,
    onToggleComment: (Boolean) -> Unit,
    onToggleLike: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
            }
            Text("알림 설정 🔔", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("댓글 알림", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        Text("내 글에 댓글이 달리면 알림을 받습니다.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.commentNotification,
                        onCheckedChange = onToggleComment,
                        colors = SwitchDefaults.colors(checkedThumbColor = PointOrange)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("좋아요 알림", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        Text("내 글에 좋아요가 눌리면 알림을 받습니다.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = profile.likeNotification,
                        onCheckedChange = onToggleLike,
                        colors = SwitchDefaults.colors(checkedThumbColor = PointOrange)
                    )
                }
            }
        }
    }
}

// ⚠️ 회원 탈퇴 안내 팝업
@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    var isAgreed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("회원 탈퇴", fontWeight = FontWeight.Bold, color = Color.Red) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "정말로 탈퇴하시겠습니까?\n\n⚠️ 작성한 글·댓글은 '탈퇴한 사용자'로 남아요.",
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 18.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isAgreed = !isAgreed }
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { isAgreed = it },
                        colors = CheckboxDefaults.colors(checkedColor = PointOrange)
                    )
                    Text("안내 사항을 확인하였으며 동의합니다.", fontSize = 12.sp, color = TextDark)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = isAgreed,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("탈퇴 확정", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = Color.Gray)
            }
        }
    )
}