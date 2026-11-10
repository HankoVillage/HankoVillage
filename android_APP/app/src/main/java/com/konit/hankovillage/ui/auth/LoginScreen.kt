package com.konit.hankovillage.ui.auth

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import com.konit.hankovillage.network.RetrofitInstance
import com.konit.hankovillage.network.SocialLoginRequest
import com.konit.hankovillage.ui.profile.UserProfileData
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.GoogleBg
import com.konit.hankovillage.ui.theme.GoogleBorder
import com.konit.hankovillage.ui.theme.KakaoText
import com.konit.hankovillage.ui.theme.KakaoYellow
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 소셜 로그인 및 둘러보기 선택을 제공하는 화면 컴포저블입니다.
 *
 * @param onLoginSuccess 로그인 성공 시 세션 정보를 전달받는 콜백
 * @param onGuestMode 둘러보기 선택 시 실행할 콜백
 * @param onSignupRequired 신규 회원가입 필요 시 signup_token을 전달하는 콜백
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (UserSession) -> Unit,
    onGuestMode: () -> Unit,
    onSignupRequired: (signupToken: String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = PointOrange)
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    isLoading = true
                    handleKakaoLogin(
                        context = context,
                        coroutineScope = coroutineScope,
                        onSuccess = { userId, nickname, profileImage ->
                            isLoading = false
                            val session = UserSession(
                                userId = userId,
                                provider = "kakao",
                                providerId = userId,
                                profile = UserProfileData(
                                    nickname = nickname,
                                    bio = "카카오로 로그인한 뜨개이웃",
                                    profileImageUri = profileImage
                                )
                            )
                            Toast.makeText(context, "${nickname}님 환영합니다!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess(session)
                        },
                        onNewUser = { signupToken ->
                            isLoading = false
                            onSignupRequired(signupToken)
                        },
                        onFailure = { message ->
                            isLoading = false
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isLoading,
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

            OutlinedButton(
                onClick = {
                    isLoading = true
                    handleGoogleLogin(
                        context = context,
                        onSuccess = { email, nickname, profileImage ->
                            isLoading = false
                            val session = UserSession(
                                userId = email,
                                provider = "google",
                                providerId = email,
                                profile = UserProfileData(
                                    nickname = nickname,
                                    bio = "구글 계정으로 시작한 뜨개이웃",
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
                enabled = !isLoading,
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

/**
 * 카카오 인증 후 백엔드 API와 통신하여 사용자 세션을 처리하는 핸들러입니다.
 */
private fun handleKakaoLogin(
    context: Context,
    coroutineScope: CoroutineScope,
    onSuccess: (userId: String, nickname: String, profileImage: String?) -> Unit,
    onNewUser: (signupToken: String) -> Unit,
    onFailure: (String) -> Unit
) {
    val apiService = RetrofitInstance.getApiService(context)

    val callback: (OAuthToken?, Throwable?) -> Unit = callback@{ token, error ->
        if (error != null) {
            Log.e("KakaoAuth", "카카오 SDK 로그인 실패", error)
            onFailure("카카오 로그인에 실패했습니다.")
        } else if (token != null) {
            val idToken = token.idToken

            // 백엔드의 OIDC ID Token 검증을 위해 idToken이 필수이므로 유효성을 우선 확인합니다.
            if (idToken == null) {
                Log.e("KakaoAuth", "idToken이 null입니다. 카카오 콘솔 OIDC 설정을 확인하세요.")
                onFailure("카카오 인증 설정을 확인해 주세요.")
                return@callback
            }

            coroutineScope.launch {
                try {
                    val response = apiService.socialLoginKakao(SocialLoginRequest(token = idToken))
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!

                        // 백엔드 판별 결과 신규 회원일 경우 약관 동의 및 프로필 설정을 위해 가입 화면으로 전환합니다.
                        if (body.isNewUser) {
                            val signupToken = body.signupToken
                            if (signupToken != null) {
                                onNewUser(signupToken)
                            } else {
                                onFailure("가입 토큰을 받지 못했습니다.")
                            }
                        } else {
                            // 기존 회원은 보안 인증 토큰을 앱 로컬 스토리지에 저장 후 세션을 시작합니다.
                            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("access_token", body.accessToken)
                                .putString("refresh_token", body.refreshToken)
                                .apply()

                            onSuccess(
                                body.user?.id?.toString() ?: "",
                                body.user?.nickname ?: "뜨개이웃",
                                null
                            )
                        }
                    } else {
                        when (response.code()) {
                            401 -> onFailure("인증에 실패했습니다. 카카오 로그인을 다시 시도해 주세요.")
                            409 -> onFailure("이미 가입된 계정입니다. 카카오 로그인을 다시 시도해 주세요.")
                            503 -> onFailure("잠시 후 다시 시도해 주세요.")
                            else -> onFailure("로그인 실패 (${response.code()})")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("KakaoAuth", "서버 로그인 통신 오류", e)
                    onFailure("서버 통신이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.")
                }
            }
        }
    }

    UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
}

/**
 * 구글 소셜 로그인 연동 핸들러입니다.
 */
private fun handleGoogleLogin(
    context: Context,
    onSuccess: (email: String, nickname: String, profileImage: String?) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    onSuccess("gmail_knitter@gmail.com", "G메일뜨개러", null)
}