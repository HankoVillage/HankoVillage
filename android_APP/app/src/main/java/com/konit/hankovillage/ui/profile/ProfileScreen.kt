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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
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

                        val isEditProfileEnabled = false

                        OutlinedButton(
                            onClick = {
                                if (isEditProfileEnabled) {
                                    currentSubScreen = ProfileSubScreen.EDIT_PROFILE
                                }
                            },
                            enabled = isEditProfileEnabled,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = PointOrange,
                                disabledContentColor = Color.Gray
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = if (isEditProfileEnabled) PointOrange else Color.Gray.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (isEditProfileEnabled) "프로필 수정" else "프로필 수정 (준비 중)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                            title = "작성한 글",
                            onClick = { currentSubScreen = ProfileSubScreen.MY_POSTS }
                        )
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                        ProfileMenuItem(
                            icon = Icons.Default.Favorite,
                            title = "좋아요한 글",
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

                if (response.code() == 204) {
                    when (provider) {
                        "kakao" -> {
                            UserApiClient.instance.unlink { _ ->
                                onSuccess()
                            }
                        }
                        "google" -> {
                            try {
                                CredentialManager.create(context)
                                    .clearCredentialState(ClearCredentialStateRequest())
                            } catch (_: Exception) {
                            }
                            onSuccess()
                        }
                        else -> onSuccess()
                    }
                } else {
                    val errorJson = response.errorBody()?.string()
                    val detailMessage = try {
                        JSONObject(errorJson ?: "").optString("detail")
                    } catch (_: Exception) {
                        ""
                    }

                    when (response.code()) {
                        403 -> {
                            val msg = detailMessage.ifEmpty { "다시 로그인해 주세요." }
                            onReauthFailed(msg)
                        }
                        401 -> {
                            val msg = detailMessage.ifEmpty { "인증이 만료되었습니다. 다시 시도해 주세요." }
                            onFailure(msg)
                        }
                        else -> {
                            val msg = detailMessage.ifEmpty { "탈퇴 처리에 실패했습니다. (${response.code()})" }
                            onFailure(msg)
                        }
                    }
                }
            } catch (e: Exception) {
                onFailure("네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
            }
        }
    }

    when (provider) {
        "kakao" -> {
            val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                if (error != null || token?.idToken == null) {
                    onFailure("카카오 재인증에 실패했습니다.")
                } else {
                    requestWithdrawToServer(token.idToken!!)
                }
            }

            if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                    if (error != null) {
                        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                            onFailure("카카오 재인증이 취소되었습니다.")
                        } else {
                            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
                        }
                    } else {
                        callback(token, null)
                    }
                }
            } else {
                UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
            }
        }

        "google" -> {
            val credentialManager = CredentialManager.create(context)
            val nonce = java.util.UUID.randomUUID().toString()
            val signInWithGoogleOption = com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(
                serverClientId = context.getString(R.string.default_web_client_id)
            )
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            coroutineScope.launch {
                try {
                    try {
                        credentialManager.clearCredentialState(
                            androidx.credentials.ClearCredentialStateRequest()
                        )
                    } catch (_: Exception) {}

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

        else -> onFailure("지원하지 않는 로그인 방식입니다.")
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = titleColor,
                fontWeight = FontWeight.Medium
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
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
                        textAlign = TextAlign.End,
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
                    textAlign = TextAlign.End,
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
    onBack: () -> Unit,
    isComingSoon: Boolean = true
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

        if (isComingSoon) {
            val descriptionTitle = if (title.contains("좋아요")) {
                "좋아요한 게시글 모아보기 기능을\n열심히 준비하고 있어요!"
            } else {
                "내가 작성한 게시글 모아보기 기능을\n열심히 준비하고 있어요!"
            }

            FeatureComingSoonCard(
                icon = if (title.contains("좋아요")) Icons.Default.FavoriteBorder else Icons.Default.EditNote,
                headline = descriptionTitle,
                subText = "커뮤니티 오픈과 함께 다음 학기 업데이트에서\n활동 내역을 확인하실 수 있습니다."
            )
            return@Column
        }

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
    onToggleLike: (Boolean) -> Unit,
    isComingSoon: Boolean = true
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

        if (isComingSoon) {
            FeatureComingSoonCard(
                icon = Icons.Default.NotificationsNone,
                headline = "댓글·좋아요 푸시 알림 기능을\n열심히 준비하고 있어요!",
                subText = "커뮤니티 오픈과 함께 다음 학기 업데이트에서\n알림 설정을 이용하실 수 있습니다."
            )
            return@Column
        }

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
 * 다음 학기 제공 예정 기능 공통 안내 카드 컴포저블
 */
@Composable
fun FeatureComingSoonCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    headline: String,
    subText: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = PointOrange.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = PointOrange,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Surface(
                    color = PointOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "다음 학기 제공 예정",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PointOrange,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = headline,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Text(
                    text = subText,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
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
                    "정말로 탈퇴하시겠습니까?\n\n" +
                            "작성한 글과 댓글은 삭제되지 않고 '탈퇴한 사용자'로 남습니다.\n" +
                            "탈퇴를 진행하려면 본인 확인을 위해 다시 로그인해야 합니다.\n" +
                            "탈퇴 후에는 작성자 확인이 어려워 글 삭제 요청이 제한될 수 있습니다.\n" +
                            "지우고 싶은 글은 탈퇴 전에 미리 삭제해 주시기 바랍니다.",
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