package com.konit.hankovillage.ui.profile

/**
 * 사용자 프로필 데이터 클래스 (서버 422 에러 방지를 위한 닉네임/자기소개 글자 수 제약 검증 포함)
 */
data class UserProfileData(
    var nickname: String = "김뜨개",
    var bio: String = "뜨개질 2년 차 가을 니트 뜨는 중!",
    var profileImageUri: String? = null,
    var commentNotification: Boolean = true,
    var likeNotification: Boolean = true
) {
    init {
        if (nickname.length > 10) {
            nickname = nickname.take(10)
        }
        if (bio.length > 30) {
            bio = bio.take(30)
        }
    }
}

/**
 * 마이페이지 완성작 데이터 클래스
 */
data class CompletedWork(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    val imageUrl: String? = null
) {
    init {
        if (title.length > 50) {
            title = title.take(50)
        }
    }
}