package com.konit.hankovillage.ui.counter

import androidx.compose.runtime.saveable.Saver

/**
 * 프로젝트 카운터 데이터 모델
 */
data class ProjectData(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val needleType: String,
    val yarnAndNeedle: String,
    val currentRows: Int,
    val totalRows: Int,
    val status: String,
    val memo: String
)

/**
 * Nullable ProjectData? 전용 Saver (rememberSaveable 타입 에러 완벽 해결)
 */
val ProjectDataSaver = Saver<ProjectData?, List<Any>>(
    save = { project ->
        if (project == null) emptyList()
        else listOf(
            project.id,
            project.title,
            project.needleType,
            project.yarnAndNeedle,
            project.currentRows,
            project.totalRows,
            project.status,
            project.memo
        )
    },
    restore = { list ->
        if (list.isEmpty()) null
        else ProjectData(
            id = list[0] as Long,
            title = list[1] as String,
            needleType = list[2] as String,
            yarnAndNeedle = list[3] as String,
            currentRows = list[4] as Int,
            totalRows = list[5] as Int,
            status = list[6] as String,
            memo = list[7] as String
        )
    }
)