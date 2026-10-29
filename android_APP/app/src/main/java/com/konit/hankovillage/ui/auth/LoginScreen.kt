package com.konit.hankovillage.ui.auth

import android.content.Context
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
import com.konit.hankovillage.ui.profile.UserProfileData
import com.konit.hankovillage.ui.theme.BgColor
import com.konit.hankovillage.ui.theme.GoogleBg
import com.konit.hankovillage.ui.theme.GoogleBorder
import com.konit.hankovillage.ui.theme.KakaoText
import com.konit.hankovillage.ui.theme.KakaoYellow
import com.konit.hankovillage.ui.theme.PointOrange
import com.konit.hankovillage.ui.theme.TextDark


/**
 * 소셜 로그인 및 둘러보기 선택을 제공하는 화면 컴포저블
 *
 * @param onLoginSuccess 로그인 성공 시 세션 정보를 전달받는 콜백
 * @param onGuestMode 둘러보기 선택 시 실행할 콜백
 */
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
                        onSuccess = { email, nickname, profileImage ->
                            isLoading = false
                            val session = UserSession(
                                userId = email,
                                provider = "kakao",
                                providerId = email,
                                profile = UserProfileData(
                                    nickname = nickname,
                                    bio = "카카오로 로그인한 뜨개이웃",
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
 * 카카오 소셜 로그인 연동 핸들러
 */
private fun handleKakaoLogin(
    context: Context,
    onSuccess: (email: String, nickname: String, profileImage: String?) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    onSuccess("kakao_knitter@kakao.com", "카카오뜨개러", null)
}

/**
 * 구글 소셜 로그인 연동 핸들러
 */
private fun handleGoogleLogin(
    context: Context,
    onSuccess: (email: String, nickname: String, profileImage: String?) -> Unit,
    onFailure: (Throwable) -> Unit
) {
    onSuccess("gmail_knitter@gmail.com", "G메일뜨개러", null)
}