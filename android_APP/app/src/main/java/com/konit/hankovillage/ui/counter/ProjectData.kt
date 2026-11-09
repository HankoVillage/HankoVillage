package com.konit.hankovillage.ui.counter

import androidx.compose.runtime.saveable.Saver
import com.google.gson.annotations.SerializedName

/**
 * 백엔드 RESTful API와 1:1 매핑되는 뜨개질 프로젝트 데이터 모델입니다.
 *
 * @property id 프로젝트 식별자
 * @property name 프로젝트 명칭
 * @property needleType 바늘 종류 코드 ("knitting", "crochet", "both")
 * @property needle 사용 실 및 바늘 정보
 * @property yarnId 연동된 실 ID
 * @property currentRows 현재 진행 단수
 * @property targetRows 목표 단수 (null인 경우 목표 없음)
 * @property status 진행 상태 코드 ("in_progress", "done")
 */
data class ProjectData(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("name")
    var name: String,

    @SerializedName("needle_type")
    var needleType: String = "knitting",

    @SerializedName("needle")
    var needle: String? = null,

    @SerializedName("yarn_id")
    var yarnId: Int? = null,

    @SerializedName("current_row")
    var currentRows: Int = 0,

    @SerializedName("target_row")
    var targetRows: Int? = null,

    @SerializedName("status")
    var status: String = "in_progress"
) {
    /**
     * DB 바늘 종류 코드 값을 화면에 노출할 한글 명칭으로 변환합니다.
     */
    fun getNeedleTypeDisplay(): String {
        return when (needleType) {
            "crochet" -> "코바늘"
            "both" -> "둘 다"
            else -> "대바늘"
        }
    }

    /**
     * DB 상태 코드 값을 화면에 노출할 한글 명칭으로 변환합니다.
     */
    fun getStatusDisplay(): String {
        return when (status) {
            "done" -> "완성"
            else -> "진행"
        }
    }

    companion object {
        /**
         * UI에서 선택한 한글 바늘 종류를 서버 전달용 코드 값으로 변환합니다.
         */
        fun toServerNeedleType(koreanText: String): String {
            return when (koreanText) {
                "코바늘" -> "crochet"
                "둘 다" -> "both"
                else -> "knitting"
            }
        }

        /**
         * UI에서 선택한 한글 상태를 서버 전달용 코드 값으로 변환합니다.
         */
        fun toServerStatus(koreanText: String): String {
            return when (koreanText) {
                "완성", "완료" -> "done"
                else -> "in_progress"
            }
        }
    }
}

/**
 * [ProjectData]의 Nullable 객체 재구성을 위한 Composable State Saver입니다.
 */
val ProjectDataSaver = Saver<ProjectData?, List<Any?>>(
    save = { project ->
        if (project == null) emptyList()
        else listOf(
            project.id,
            project.name,
            project.needleType,
            project.needle,
            project.yarnId,
            project.currentRows,
            project.targetRows,
            project.status
        )
    },
    restore = { list ->
        if (list.isEmpty()) null
        else ProjectData(
            id = list[0] as Long,
            name = list[1] as String,
            needleType = list[2] as String,
            needle = list[3] as? String,
            yarnId = list[4] as? Int,
            currentRows = list[5] as Int,
            targetRows = list[6] as? Int,
            status = list[7] as String
        )
    }
)

/**
 * 서버에 현재 단수 업데이트를 요청할 때 사용하는 데이터 전송 객체(DTO)입니다.
 *
 * @property currentRow 변경할 현재 단수
 */
data class ProjectCounterUpdateRequest(
    @SerializedName("current_row")
    val currentRow: Int
)