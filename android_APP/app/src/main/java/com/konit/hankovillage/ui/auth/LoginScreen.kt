package com.konit.hankovillage.ui.auth

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konit.hankovillage.ui.profile.UserProfileData

val BgColor = Color(0xFFF7F4EB)
val PointOrange = Color(0xFFD36D33)
val TextDark = Color(0xFF332D29)

// 소셜 브랜드 색상
val KakaoYellow = Color(0xFFFEE500)
val KakaoText = Color(0xFF191919)
val GoogleBg = Color(0xFFFFFFFF)
val GoogleBorder = Color(0xFFDADCE0)

@Composable
fun LoginScreen(
    onLoginSuccess: (UserSession) -> Unit,
    onGuestMode: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 🧶 1. 앱 로고 및 환영 문구 영역
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🧶",
                fontSize = 64.sp
            )
            Text(
                text = "한코마을",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "뜨개 이웃들과 함께 만들어가는 공간",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // 📲 2. 소셜 로그인 버튼 영역 (카카오 & 구글)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PointOrange)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 💛 카카오 로그인 버튼
            Button(
                onClick = {
                    isLoading = true
                    handleKakaoLogin(
                        context = context,
                        onSuccess = { email, nickname, profileImage ->
                            isLoading = false
                            val session = UserSession(
                                userId = email,
                                profile = UserProfileData(
                                    nickname = nickname,
                                    bio = "카카오로 로그인한 뜨개이웃 🧶",
                                    profileImageUri = profileImage
                                )
                            )
                            Toast.makeText(context, "${nickname}님 환영합니다!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(session)
                        },
                        onFailure = {
                            isLoading = false
                            Toast.makeText(context, "카카오 로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KakaoYellow,
                    contentColor = KakaoText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "카카오로 시작하기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KakaoText
                    )
                }
            }

            // 🔴 구글(G메일) 로그인 버튼
            OutlinedButton(
                onClick = {
                    isLoading = true
                    handleGoogleLogin(
                        context = context,
                        onSuccess = { email, nickname, profileImage ->
                            isLoading = false
                            val session = UserSession(
                                userId = email,
                                profile = UserProfileData(
                                    nickname = nickname,
                                    bio = "구글 계정으로 시작한 뜨개이웃 🧶",
                                    profileImageUri = profileImage
                                )
                            )
                            Toast.makeText(context, "${nickname}님 환영합니다!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(session)
                        },
                        onFailure = {
                            isLoading = false
                            Toast.makeText(context, "구글 로그인 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoogleBg,
                    contentColor = TextDark
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoogleBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Text(
                        text = "Google 계정으로 시작하기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🚶‍♂️ 둘러보기 (비회원 로그인)
            Text(
                text = "로그인 없이 둘러보기",
                fontSize = 13.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onGuestMode() }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// 💬 카카오 로그인 연동 연동 함수
private fun handleKakaoLogin(
    context: Context,
    onSuccess: (email: String, nickname: String, profileImage: String?) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    /*
     * 💡 실제 Kakao SDK 연동 시 예시:
     *
     * val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
     *     if (error != null) {
     *         onFailure(error)
     *     } else if (token != null) {
     *         UserApiClient.instance.me { user, _ ->
     *             val email = user?.kakaoAccount?.email ?: "kakao_user@hankovillage.com"
     *             val nickname = user?.kakaoAccount?.profile?.nickname ?: "카카오뜨개이웃"
     *             val profileImage = user?.kakaoAccount?.profile?.profileImageUrl
     *             onSuccess(email, nickname, profileImage)
     *         }
     *     }
     * }
     *
     * if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
     *     UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
     * } else {
     *     UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
     * }
     */

    // 🧪 테스트 더미 동작 (SDK 등록 전 시뮬레이션용)
    onSuccess("kakao_knitter@kakao.com", "카카오뜨개러", null)
}

// 🔴 구글 로그인 연동 함수 (Credential Manager API 기준)
private fun handleGoogleLogin(
    context: Context,
    onSuccess: (email: String, nickname: String, profileImage: String?) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    /*
     * 💡 실제 Google Credential Manager SDK 연동 시:
     *
     * val credentialManager = CredentialManager.create(context)
     * val rawNonce = UUID.randomUUID().toString()
     * val bytes = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
     * val hashedNonce = bytes.fold("") { str, it -> str + "%02x".format(it) }
     *
     * val googleIdOption = GetGoogleIdOption.Builder()
     *     .setFilterByAuthorizedAccounts(false)
     *     .setServerClientId("YOUR_GOOGLE_WEB_CLIENT_ID")
     *     .setNonce(hashedNonce)
     *     .build()
     * ...
     */

    // 🧪 테스트 더미 동작 (SDK 등록 전 시뮬레이션용)
    onSuccess("gmail_knitter@gmail.com", "G메일뜨개러", null)
}