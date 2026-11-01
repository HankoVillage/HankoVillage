package com.konit.hankovillage.ui.pattern

/**
 * 도안 진행 상태 정의
 */
enum class PatternStatus(val label: String) {
    PLANNED("예정"),
    IN_PROGRESS("진행"),
    COMPLETED("완성")
}

/**
 * 도안 정렬 순서 정의
 */
enum class PatternSortOrder(val label: String) {
    NEWEST("최신순"),
    TITLE("제목순")
}

/**
 * 도안 데이터 클래스
 */
data class PatternData(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var tag: String,              // 의류, 목도리, 모자, 가방, 인형, 기타
    var yarnInfo: String = "",    // 실 정보
    var patternUrl: String = "",  // 도안 링크 URL
    var imageUrl: String? = null,
    var status: PatternStatus = PatternStatus.PLANNED
) {
    init {
        if (title.length > 50) {
            title = title.take(50)
        }
        if (yarnInfo.length > 50) {
            yarnInfo = yarnInfo.take(50)
        }
    }
}