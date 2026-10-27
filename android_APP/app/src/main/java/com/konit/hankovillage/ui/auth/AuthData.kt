package com.konit.hankovillage.ui.auth

import com.konit.hankovillage.ui.profile.UserProfileData

// 패키지 경로 예시

data class UserSession(
    val userId: String,
    val provider: String,
    val providerId: String,
    val profile: UserProfileData
)