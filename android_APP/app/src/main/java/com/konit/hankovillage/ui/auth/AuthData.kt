package com.konit.hankovillage.ui.auth

import com.konit.hankovillage.ui.profile.UserProfileData

// 현재 로그인한 사용자 정보 (상수값이 아닌 변수로 관리)
data class UserSession(
    val userId: String,
    var profile: UserProfileData,
    val isLoggedIn: Boolean = true
)