package com.konit.hankovillage.ui.profile

data class UserProfileData(
    var nickname: String = "김뜨개",
    var bio: String = "뜨개질 2년 차 🧶 가을 니트 뜨는 중!",
    var profileImageUri: String? = null,
    var commentNotification: Boolean = true,
    var likeNotification: Boolean = true
)

// 완성작 갤러리용 데이터 모델 (이 파일에만 선언)
data class CompletedWork(
    val id: Long,
    val title: String,
    val imageUrl: String? = null
)