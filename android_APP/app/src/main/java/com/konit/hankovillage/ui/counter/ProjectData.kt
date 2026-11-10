package com.konit.hankovillage.ui.counter

import com.google.gson.annotations.SerializedName
import androidx.compose.runtime.saveable.Saver

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
    var yarnId: Long? = null,

    @SerializedName("current_row")
    var currentRows: Int = 0,

    @SerializedName("target_row")
    var targetRows: Int? = null,

    @SerializedName("status")
    var status: String = "in_progress",
) {
    fun getNeedleTypeDisplay(): String {
        return when (needleType) {
            "crochet" -> "코바늘"
            "both" -> "둘 다"
            else -> "대바늘"
        }
    }

    fun getStatusDisplay(): String {
        return when (status) {
            "done" -> "완성"
            else -> "진행"
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

        fun toServerStatus(koreanText: String): String {
            return when (koreanText) {
                "완성", "완료" -> "done"
                else -> "in_progress"
            }
        }
    }
}
data class ProjectCounterUpdateRequest(
    @SerializedName("current_row")
    val currentRow: Int
)
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
            yarnId = list[4] as? Long,
            currentRows = list[5] as Int,
            targetRows = list[6] as? Int,
            status = list[7] as String
        )
    }
)