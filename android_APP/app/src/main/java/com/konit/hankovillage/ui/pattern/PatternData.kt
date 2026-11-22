package com.konit.hankovillage.ui.pattern

import com.google.gson.annotations.SerializedName

/**
 * 도안 진행 상태 정의 (UI 한글 라벨 <-> 서버 DB 코드 매핑)
 */
enum class PatternStatus(val label: String, val serverCode: String) {
    PLANNED("예정", "planned"),
    IN_PROGRESS("진행", "in_progress"),
    COMPLETED("완성", "done");

    companion object {
        fun fromServerCode(code: String?): PatternStatus {
            return entries.find { it.serverCode == code } ?: PLANNED
        }
    }
}

/**
 * 도안 정렬 순서 정의
 */
enum class PatternSortOrder(val label: String, val serverSort: String) {
    NEWEST("최신순", "NEWEST"),
    TITLE("제목순", "TITLE")
}

/**
 * 서버 응답(GET / POST / PATCH /patterns) 및 앱 화면 공용 도안 데이터 모델
 */
data class PatternData(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("title")
    var title: String,

    @SerializedName("tag")
    var tagCode: String = "etc", // 서버 저장용 영문 코드 ("clothing", "scarf", "hat", "bag", "doll", "etc")

    @SerializedName("yarn_info")
    var yarnInfo: String? = null, // 사용 실 정보 (최대 50자)

    @SerializedName("link_url")
    var patternUrl: String? = null, // 도안 링크 URL (최대 2048자)

    @SerializedName("finished_photo_key")
    var imageUrl: String? = null, // 완성작 사진 키/경로

    @SerializedName("status")
    var statusCode: String = "planned" // 서버 저장용 상태 코드 ("planned", "in_progress", "done")
) {
    init {
        if (title.length > 50) {
            title = title.take(50)
        }
        if ((yarnInfo?.length ?: 0) > 50) {
            yarnInfo = yarnInfo?.take(50)
        }
    }

    // UI 표시용 한글 태그 반환
    val tag: String
        get() = toKoreanTag(tagCode)

    // UI 표시용 Enum 상태 반환
    val status: PatternStatus
        get() = PatternStatus.fromServerCode(statusCode)

    companion object {
        fun toKoreanTag(code: String?): String = when (code) {
            "clothing" -> "의류"
            "scarf" -> "목도리"
            "hat" -> "모자"
            "bag" -> "가방"
            "doll" -> "인형"
            else -> "기타"
        }

        fun toServerTag(koreanTag: String): String = when (koreanTag) {
            "의류" -> "clothing"
            "목도리" -> "scarf"
            "모자" -> "hat"
            "가방" -> "bag"
            "인형" -> "doll"
            else -> "etc"
        }
    }
}

/**
 * POST /patterns 등록 요청용 DTO
 */
data class PatternCreateRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("tag")
    val tag: String,

    @SerializedName("status")
    val status: String = "planned",

    @SerializedName("link_url")
    val linkUrl: String? = null,

    @SerializedName("finished_photo_key")
    val finishedPhotoKey: String? = null,

    @SerializedName("yarn_info")
    val yarnInfo: String? = null
)

/**
 * PATCH /patterns/{id} 수정 요청용 DTO
 */
data class PatternUpdateRequest(
    @SerializedName("title")
    val title: String? = null,

    @SerializedName("tag")
    val tag: String? = null,

    @SerializedName("status")
    val status: String? = null,

    @SerializedName("link_url")
    val linkUrl: String? = null,

    @SerializedName("finished_photo_key")
    val finishedPhotoKey: String? = null,

    @SerializedName("yarn_info")
    val yarnInfo: String? = null
)