package com.konit.hankovillage.ui.auth

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
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
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SignupRequest
import com.konit.hankovillage.ui.pattern.UriImage
import com.konit.hankovillage.ui.profile.UserProfileData
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.CardBg
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark
import com.konit.hankovillage.ui.util.ImageCropDialog
import com.konit.hankovillage.ui.util.deleteCroppedImageFile
import kotlinx.coroutines.launch

/**
 * 카카오 인증 후 회원 프로필 설정 및 필수 약관 동의를 진행하는 회원가입 화면 컴포저블입니다.
 *
 * @param signupToken 카카오 인증 성공 후 전달받은 회원가입 임시 토큰
 * @param onSignupSuccess 회원가입 완료 및 세션 생성 완료 시 호출되는 콜백
 * @param onCancelOrExpired 가입 취소 또는 토큰 만료 시 이전 로그인 화면으로 이동하는 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    signupToken: String,
    onSignupSuccess: (UserSession) -> Unit,
    onCancelOrExpired: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { RetrofitInstance.getApiService(context) }

    var nickname by remember { mutableStateOf("") }
    var nicknameError by remember { mutableStateOf<String?>(null) }
    var bio by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    var isTermsAgreed by remember { mutableStateOf(false) }
    var isPrivacyAgreed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var termsDialogTitle by remember { mutableStateOf<String?>(null) }
    var termsDialogContent by remember { mutableStateOf("") }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingCropUri = it }
    }

    val nicknameRegex = remember { Regex("^[가-힣a-zA-Z0-9]{2,10}$") }
    val canSubmit = nicknameRegex.matches(nickname) && isTermsAgreed && isPrivacyAgreed && !isLoading

    Scaffold(
        containerColor = BgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text("회원가입", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                },
                navigationIcon = {
                    IconButton(onClick = onCancelOrExpired) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgColor)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text = "한코마을에서 사용할\n프로필을 설정해 주세요",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    lineHeight = 30.sp
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(CardBg)
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
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "기본 프로필",
                                tint = Color.Gray,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(PointOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "사진 변경",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("닉네임 *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                nickname = input
                                nicknameError = when {
                                    input.isEmpty() -> null
                                    !nicknameRegex.matches(input) -> "한글(완성형), 영문, 숫자 2~10자만 가능해요"
                                    else -> null
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("2~10자 입력", fontSize = 14.sp) },
                        singleLine = true,
                        isError = nicknameError != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PointOrange,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    if (nicknameError != null) {
                        Text(text = nicknameError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("자기소개 (선택)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 30) bio = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("간단한 소개글을 적어주세요 (최대 30자)", fontSize = 14.sp) },
                        supportingText = {
                            Text(
                                text = "${bio.length}/30자",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PointOrange,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !(isTermsAgreed && isPrivacyAgreed)
                                isTermsAgreed = next
                                isPrivacyAgreed = next
                            }
                    ) {
                        Checkbox(
                            checked = isTermsAgreed && isPrivacyAgreed,
                            onCheckedChange = {
                                isTermsAgreed = it
                                isPrivacyAgreed = it
                            },
                            colors = CheckboxDefaults.colors(checkedColor = PointOrange)
                        )
                        Text("전체 약관에 동의합니다", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.6f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTermsAgreed = !isTermsAgreed }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isTermsAgreed,
                                onCheckedChange = { isTermsAgreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = PointOrange)
                            )
                            Text("[필수] 서비스 이용약관 동의", fontSize = 13.sp, color = TextDark)
                        }
                        Text(
                            text = "보기",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable {
                                    termsDialogTitle = "서비스 이용약관"
                                    termsDialogContent = LegalTexts.TERMS_OF_SERVICE
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPrivacyAgreed = !isPrivacyAgreed }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isPrivacyAgreed,
                                onCheckedChange = { isPrivacyAgreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = PointOrange)
                            )
                            Text("[필수] 개인정보 처리방침 동의", fontSize = 13.sp, color = TextDark)
                        }
                        Text(
                            text = "보기",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .clickable {
                                    termsDialogTitle = "개인정보 처리방침"
                                    termsDialogContent = LegalTexts.PRIVACY_POLICY
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val response = apiService.signup(
                                SignupRequest(
                                    signupToken = signupToken,
                                    nickname = nickname,
                                    termsAgreed = true
                                )
                            )
                            isLoading = false

                            if (response.isSuccessful && response.body() != null) {
                                val body = response.body()!!
                                context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("access_token", body.accessToken)
                                    .putString("refresh_token", body.refreshToken)
                                    .apply()

                                val newSession = UserSession(
                                    userId = body.user?.id?.toString() ?: "",
                                    provider = "kakao",
                                    providerId = body.user?.id?.toString() ?: "",
                                    profile = UserProfileData(
                                        nickname = body.user?.nickname ?: nickname,
                                        bio = bio.ifBlank { "한코마을에 새로 오신 뜨개이웃" },
                                        profileImageUri = profileImageUri
                                    )
                                )
                                Toast.makeText(context, "${nickname}님, 가입을 환영합니다!", Toast.LENGTH_SHORT).show()
                                onSignupSuccess(newSession)
                            } else {
                                val errorBody = response.errorBody()?.string() ?: ""
                                // 토큰 만료(401) 또는 중복(409) 등 백엔드 응답 상태별로 재인증을 유도하거나 닉네임 에러 표시
                                when (response.code()) {
                                    401 -> {
                                        Toast.makeText(context, "가입 시간이 만료되었습니다. 카카오 로그인을 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                                        onCancelOrExpired()
                                    }
                                    409 -> {
                                        if (errorBody.contains("NICKNAME_DUPLICATE")) {
                                            nicknameError = "이미 사용 중인 닉네임입니다."
                                        } else {
                                            Toast.makeText(context, "이미 가입된 계정입니다. 카카오 로그인을 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                                            onCancelOrExpired()
                                        }
                                    }
                                    503 -> Toast.makeText(context, "잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                                    else -> Toast.makeText(context, "가입 실패 (${response.code()})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "서버 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PointOrange,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text("가입 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    if (pendingCropUri != null) {
        ImageCropDialog(
            imageUri = pendingCropUri!!,
            onCropComplete = { croppedUri ->
                // 기존 크롭된 임시 이미지가 있는 경우 불필요한 용량 차지 방지를 위해 삭제 후 교체
                profileImageUri?.let { deleteCroppedImageFile(context, it) }
                profileImageUri = croppedUri.toString()
                pendingCropUri = null
            },
            onDismiss = {
                pendingCropUri = null
            }
        )
    }

    if (termsDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { termsDialogTitle = null },
            title = {
                Text(termsDialogTitle!!, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(termsDialogContent, fontSize = 12.sp, color = TextDark, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { termsDialogTitle = null }) {
                    Text("확인", color = PointOrange, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}