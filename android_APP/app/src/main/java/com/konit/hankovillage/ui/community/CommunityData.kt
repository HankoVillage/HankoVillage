package com.konit.hankovillage.ui.community

enum class CommunityCategory(val label: String) {
    ALL("전체"),
    SHOWOFF("뽐내기"),
    QUESTION("질문"),
    TIIPS("꿀팁/공유"),
    FREE("자유")
}

data class CommentData(
    val id: Long = System.currentTimeMillis(),
    val authorName: String,
    var content: String,
    val createdAt: String,
    val isMyComment: Boolean = false
)

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
)