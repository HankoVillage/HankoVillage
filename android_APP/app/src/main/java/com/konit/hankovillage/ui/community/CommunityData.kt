package com.konit.hankovillage.ui.community

/**
 * 커뮤니티 게시글 카테고리 정의
 */
enum class CommunityCategory(val label: String) {
    ALL("전체"),
    SHOWOFF("뽐내기"),
    QUESTION("질문"),
    TIPS("꿀팁/공유"),
    FREE("자유")
}

/**
 * 댓글 데이터 클래스 (서버 422 에러 방지를 위해 내용 200자 제한 검증 포함)
 */
data class CommentData(
    val id: Long = System.currentTimeMillis(),
    val authorName: String,
    var content: String,
    val createdAt: String,
    val isMyComment: Boolean = false
) {
    init {
        // 댓글은 최대 200자 초과 시 방어 처리
        if (content.length > 200) {
            content = content.take(200)
        }
    }
}

/**
 * 게시글 데이터 클래스 (서버 422 에러 방지를 위해 제목 100자 제약 검증 포함)
 */
data class PostData(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var category: CommunityCategory,
    val authorName: String,
    val createdAt: String,
    var content: String,
    var imageUrl: String? = null,
    var likeCount: Int = 0,
    var isLiked: Boolean = false,
    val comments: MutableList<CommentData> = mutableListOf(),
    val isMyPost: Boolean = false
) {
    init {
        // 게시글 제목은 최대 100자 초과 시 방어 처리
        if (title.length > 100) {
            title = title.take(100)
        }
    }
}