package com.konit.hankovillage.ui.profile

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.user.UserApiClient
import com.konit.hankovillage.R
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SocialLoginRequest
import com.konit.hankovillage.ui.community.PostData
import com.konit.hankovillage.ui.pattern.PatternData
import com.konit.hankovillage.ui.pattern.PatternStatus
import com.konit.hankovillage.ui.pattern.UriImage
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark
import com.konit.hankovillage.ui.util.ImageCropDialog
import com.konit.hankovillage.ui.util.deleteCroppedImageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 마이페이지 내 세부 서브 화면 이동을 위한 내비게이션 상태 정의
 */
enum class ProfileSubScreen {
    NONE, EDIT_PROFILE, MY_POSTS, LIKED_POSTS, NOTIFICATION_SETTINGS
}

/**
 * 마이페이지 메인 화면 컴포저블
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfileData = UserProfileData(),
    provider: String = "",
    postList: List<PostData> = emptyList(),
    patternList: List<PatternData> = emptyList(),
    onUpdateProfile: (UserProfileData) -> Unit = {},
    onLogout: () -> Unit = {},
    onWithdrawSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var profile by remember(userProfile) { mutableStateOf(userProfile) }

    // SnapshotStateList 내부 변경까지 즉시 반영되도록 derivedStateOf 사용
    val completedWorks by remember(patternList) {
        derivedStateOf {
            patternList.filter { it.status == PatternStatus.COMPLETED }
        }
    }
    val myPosts by remember(postList) {
        derivedStateOf {
            postList.filter { it.isMyPost || it.authorName == profile.nickname }
        }
    }
    val likedPosts by remember(postList) {
        derivedStateOf {
            postList.filter { it.isLiked }
        }
    }

    var currentSubScreen by remember {
        mutableStateOf(
            if (userProfile.nickname.isBlank() || userProfile.nickname == "카카오뜨개러" || userProfile.nickname == "G메일뜨개러") {
                ProfileSubScreen.EDIT_PROFILE
            } else {
                ProfileSubScreen.NONE
            }
        )
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isWithdrawing by remember { mutableStateOf(false) }

    // EDIT_PROFILE은 내부 BackHandler에서 임시 파일 정리 후 뒤로가기 처리
    if (currentSubScreen != ProfileSubScreen.NONE && currentSubScreen != ProfileSubScreen.EDIT_PROFILE) {
        BackHandler { currentSubScreen = ProfileSubScreen.NONE }
    }

    when (currentSubScreen) {
        ProfileSubScreen.EDIT_PROFILE -> {
            EditProfileContent(
                profile = profile,
                onBack = {
                    currentSubScreen = ProfileSubScreen.NONE
                },
                onSave = { updatedProfile ->
                    val oldNickname = profile.nickname
                    profile = updatedProfile

                    if (oldNickname != updatedProfile.nickname) {
                        val stateList = postList as? androidx.compose.runtime.snapshots.SnapshotStateList<PostData>
                        if (stateList != null) {
                            stateList.indices.forEach { index ->
                                if (stateList[index].isMyPost) {
                                    stateList[index] = stateList[index].copy(authorName = updatedProfile.nickname)
                                }
                            }
                        }
                    }

                    onUpdateProfile(updatedProfile)
                    Toast.makeText(context, "프로필이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                    currentSubScreen = ProfileSubScreen.NONE
                }
            )
        }
        ProfileSubScreen.MY_POSTS -> {
            SimplePostListContent(
                title = "작성한 게시글",
                posts = myPosts,
                onBack = { currentSubScreen = ProfileSubScreen.NONE }
            )
        }
        ProfileSubScreen.LIKED_POSTS -> {
            SimplePostListContent(
                title = "좋아요한 게시글",
                posts = likedPosts,
                onBack = { currentSubScreen = ProfileSubScreen.NONE }
            )
        }
        ProfileSubScreen.NOTIFICATION_SETTINGS -> {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "내 프로필",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCD7C9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!profile.profileImageUri.isNullOrEmpty()) {
                                UriImage(
                                    uriString = profile.profileImageUri!!,
                                    contentDescription = "프로필 사진",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "기본 프로필",
                                    modifier = Modifier.size(50.dp),
                                    tint = Color.Gray
                                )
                            }
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

                Text(
                    text = "완성작 갤러리 (${completedWorks.size})",
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
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(completedWorks, key = { it.id }) { pattern ->
                            Card(
                                modifier = Modifier
                                    .width(110.dp)
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (!pattern.imageUrl.isNullOrEmpty()) {
                                        UriImage(
                                            uriString = pattern.imageUrl!!,
                                            contentDescription = pattern.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFDCD7C9)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("공란", fontSize = 24.sp)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color.Black.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = pattern.title,
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

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Default.Edit,
                            title = "작성한 글 (${myPosts.size})",
                            onClick = { currentSubScreen = ProfileSubScreen.MY_POSTS }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "좋아요한 글 (${likedPosts.size})",
                            onClick = { currentSubScreen = ProfileSubScreen.LIKED_POSTS }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "알림 설정",
                            onClick = { currentSubScreen = ProfileSubScreen.NOTIFICATION_SETTINGS }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.ExitToApp,
                            title = "로그아웃",
                            onClick = { showLogoutDialog = true }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

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

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isWithdrawing = isWithdrawing,
            onDismiss = { if (!isWithdrawing) showDeleteAccountDialog = false },
            onConfirmDelete = {
                isWithdrawing = true
                executeWithdrawal(
                    context = context,
                    provider = provider,
                    coroutineScope = coroutineScope,
                    onSuccess = {
                        deleteCroppedImageFile(context, profile.profileImageUri)
                        isWithdrawing = false
                        showDeleteAccountDialog = false
                        Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        onWithdrawSuccess()
                    },
                    onReauthFailed = { detailMessage ->
                        isWithdrawing = false
                        Toast.makeText(context, detailMessage, Toast.LENGTH_LONG).show()
                    },
                    onFailure = { errorMessage ->
                        isWithdrawing = false
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }
}

/**
 * 소셜 재인증 후 서버 회원 탈퇴(POST /users/me/withdrawal)를 처리합니다.
 */
private fun executeWithdrawal(
    context: Context,
    provider: String,
    coroutineScope: CoroutineScope,
    onSuccess: () -> Unit,
    onReauthFailed: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val apiService = RetrofitInstance.getApiService(context)

    fun requestWithdrawToServer(idToken: String) {
        coroutineScope.launch {
            try {
                val response = apiService.withdrawUser(SocialLoginRequest(token = idToken))

                when {
                    // 204 No Content: 정상 탈퇴 완료
                    response.isSuccessful -> {
                        if (provider == "kakao") {
                            UserApiClient.instance.unlink { /* 연결 끊기 완료 */ }
                        } else if (provider == "google") {
                            try {
                                CredentialManager.create(context)
                                    .clearCredentialState(ClearCredentialStateRequest())
                            } catch (_: Exception) {}
                        }
                        onSuccess()
                    }
                    // 401 Unauthorized: 이미 탈퇴된 뒤 재시도한 경우 탈퇴 완료로 처리
                    response.code() == 401 -> {
                        onSuccess()
                    }
                    // 403 REAUTH_FAILED: detail 문구 파싱 후 재로그인 유도
                    response.code() == 403 -> {
                        val errorJson = response.errorBody()?.string()
                        val detail = try {
                            JSONObject(errorJson ?: "").optString("detail")
                                .ifEmpty { "재인증에 실패했습니다. 다시 로그인해 주세요." }
                        } catch (_: Exception) {
                            "재인증에 실패했습니다. 다시 로그인해 주세요."
                        }
                        onReauthFailed(detail)
                    }
                    else -> {
                        onFailure("탈퇴 처리 실패 (${response.code()})")
                    }
                }
            } catch (e: Exception) {
                onFailure("네트워크 오류가 발생했습니다.")
            }
        }
    }

    when (provider) {
        "kakao" -> {
            UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
                if (error != null || token?.idToken == null) {
                    onFailure("카카오 재인증에 실패했습니다.")
                } else {
                    requestWithdrawToServer(token.idToken!!)
                }
            }
        }
        "google" -> {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            coroutineScope.launch {
                try {
                    val result = credentialManager.getCredential(request = request, context = context)
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        requestWithdrawToServer(idToken)
                    } else {
                        onFailure("구글 인증 정보를 가져오지 못했습니다.")
                    }
                } catch (e: Exception) {
                    onFailure("구글 재인증이 취소되었거나 실패했습니다.")
                }
            }
        }
        else -> {
            onFailure("지원하지 않는 로그인 방식입니다.")
        }
    }
}

/**
 * 프로필 메뉴 항목 아이템 컴포저블
 */
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

/**
 * 프로필 수정 화면 컴포저블 (1:1 이미지 크롭 및 임시/기존 파일 정리 연동)
 */
@Composable
fun EditProfileContent(
    profile: UserProfileData,
    onBack: () -> Unit,
    onSave: (UserProfileData) -> Unit
) {
    val context = LocalContext.current
    var nickname by remember { mutableStateOf(profile.nickname) }
    var bio by remember { mutableStateOf(profile.bio) }
    val initialImageUri = remember { profile.profileImageUri }
    var profileImageUri by remember { mutableStateOf(initialImageUri) }
    var isCheckedNickname by remember { mutableStateOf(true) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { pendingCropUri = it }
    }

    val handleBack = {
        if (profileImageUri != initialImageUri) {
            deleteCroppedImageFile(context, profileImageUri)
        }
        onBack()
    }

    BackHandler { handleBack() }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onDismiss = { pendingCropUri = null },
            onCropComplete = { croppedUri ->
                if (profileImageUri != initialImageUri) {
                    deleteCroppedImageFile(context, profileImageUri)
                }
                profileImageUri = croppedUri.toString()
                pendingCropUri = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = handleBack) {
                Text("< 취소", color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val trimmedNickname = nickname.trim()
                    when {
                        trimmedNickname.length < 2 -> {
                            Toast.makeText(context, "닉네임은 2자 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
                        }
                        !isCheckedNickname -> {
                            Toast.makeText(context, "닉네임 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            if (profileImageUri != initialImageUri) {
                                deleteCroppedImageFile(context, initialImageUri)
                            }
                            onSave(
                                profile.copy(
                                    nickname = trimmedNickname,
                                    bio = bio,
                                    profileImageUri = profileImageUri
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PointOrange),
                enabled = nickname.isNotBlank()
            ) {
                Text("저장", color = Color.White)
            }
        }

        Text("프로필 수정", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)

        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(Color(0xFFDCD7C9))
                .clickable { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (!profileImageUri.isNullOrEmpty()) {
                UriImage(
                    uriString = profileImageUri!!,
                    contentDescription = "프로필 사진",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PointOrange)
                    Text("사진 변경", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    if (it.length <= 10) {
                        nickname = it
                        isCheckedNickname = false
                    }
                },
                label = { Text("닉네임 (2~10자) *") },
                supportingText = {
                    Text(
                        text = "${nickname.length}/10자",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = {
                    if (nickname.trim().length < 2) {
                        Toast.makeText(context, "닉네임은 2자 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        isCheckedNickname = true
                        Toast.makeText(context, "사용 가능한 닉네임입니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardBg),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("중복 확인", color = TextDark, fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = bio,
            onValueChange = {
                if (it.length <= 30) bio = it
            },
            label = { Text("소개글 (최대 30자)") },
            supportingText = {
                Text(
                    text = "${bio.length}/30자",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )
    }
}

/**
 * 작성한 글 / 좋아요한 글의 실제 PostData 목록을 보여주는 컴포저블
 */
@Composable
fun SimplePostListContent(
    title: String,
    posts: List<PostData>,
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
                items(posts, key = { it.id }) { post ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = post.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${post.authorName} · ${post.createdAt}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 푸시 알림 수신 여부를 설정하는 화면 컴포저블
 */
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
            Text("알림 설정", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
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

/**
 * 회원 탈퇴 안내 및 최종 동의를 확인하는 팝업 다이얼로그
 */
@Composable
fun DeleteAccountDialog(
    isWithdrawing: Boolean = false,
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
                    "정말로 탈퇴하시겠습니까?\n\n작성한 글과 댓글은 삭제되지 않고 '탈퇴한 사용자'로 남습니다.\n탈퇴를 진행하려면 본인 확인을 위해 다시 로그인해야 합니다.",
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 18.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !isWithdrawing) { isAgreed = !isAgreed }
                ) {
                    Checkbox(
                        checked = isAgreed,
                        onCheckedChange = { if (!isWithdrawing) isAgreed = it },
                        colors = CheckboxDefaults.colors(checkedColor = PointOrange)
                    )
                    Text("안내 사항을 확인하였으며 동의합니다.", fontSize = 12.sp, color = TextDark)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                enabled = isAgreed && !isWithdrawing,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(if (isWithdrawing) "처리 중..." else "탈퇴 확정", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isWithdrawing
            ) {
                Text("취소", color = Color.Gray)
            }
        }
    )
}