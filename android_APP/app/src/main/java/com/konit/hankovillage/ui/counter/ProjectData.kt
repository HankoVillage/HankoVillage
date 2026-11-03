package com.konit.hankovillage.ui.counter

import android.os.Parcelable
import androidx.compose.runtime.saveable.Saver
import com.google.gson.annotations.SerializedName


/**
 * 백엔드 FastAPI RESTful API와 1:1 매핑되는 프로젝트(단수 카운터) 데이터 모델
 */

data class ProjectData(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("name")
    var name: String,                  // 작품 이름 (서버: name / 기존 title)

    @SerializedName("needle_type")
    var needleType: String = "대바늘",  // 대바늘 / 코바늘

    @SerializedName("needle")
    var needle: String? = null,        // 바늘 상세 정보 (서버: needle / 기존 info)

    @SerializedName("yarn_id")
    var yarnId: Int? = null,           // 연동된 실 ID

    @SerializedName("current_row")
    var currentRows: Int = 0,          // 현재 진행 단수 (서버: current_row)

    @SerializedName("target_row")
    var targetRows: Int = 100,         // 목표 단수 (서버: target_row)

    @SerializedName("status")
    var status: String = "진행"         // 상태 ("진행", "완성")
)

/**
 * Nullable ProjectData? 전용 Saver (rememberSaveable 에러 예방 및 필드명 싱크 적용)
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
            targetRows = list[6] as Int,
            status = list[7] as String
        )
    }
)

/**
 * PATCH /projects/{id}/counter 요청 전송용 DTO (단수 갱신 본문: {"current_row": 15})
 */
data class ProjectCounterUpdateRequest(
    @SerializedName("current_row")
    val currentRow: Int
)