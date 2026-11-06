package com.konit.hankovillage.ui.counter

import androidx.compose.runtime.saveable.Saver
import com.google.gson.annotations.SerializedName

/**
 * 백엔드 FastAPI RESTful API와 1:1 매핑되는 프로젝트(단수 카운터) 데이터 모델
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
    var status: String = "진행"
)
{
    fun getNeedleTypeDisplay(): String {
        return when (needleType ?: "knitting") {
            "crochet" -> "코바늘"
            "both" -> "둘 다"
            else -> "대바늘"
        }
    }

    companion object {
        fun toServerNeedleType(koreanText: String): String {
            return when (koreanText) {
                "코바늘" -> "crochet"
                "둘 다" -> "both"
                else -> "knitting"
            }
        }
    }
}


/**
 * Nullable ProjectData? 전용 Saver (rememberSaveable 적용)
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
 *  PATCH /projects/{id}/counter 요청 전송용 DTO
 */
data class ProjectCounterUpdateRequest(
    @SerializedName("current_row")
    val currentRow: Int
)