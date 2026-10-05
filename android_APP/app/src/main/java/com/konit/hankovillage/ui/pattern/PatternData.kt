package com.konit.hankovillage.ui.pattern

enum class PatternStatus(val label: String) {
    PLANNED("예정"),
    IN_PROGRESS("진행"),
    COMPLETED("완성")
}

enum class PatternSortOrder(val label: String) {
    NEWEST("최신순"),
    TITLE("제목순")
}

data class PatternData(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var tag: String,              // 의류, 목도리, 모자, 가방, 인형, 기타
    var yarnInfo: String = "",    // 실 정보
    var needleInfo: String = "",  // 바늘 정보
    var patternUrl: String = "",  // 도안 링크 URL
    var status: PatternStatus = PatternStatus.PLANNED
)