package com.konit.hankovillage.ui.counter

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 단수 카운터 및 프로젝트 관리를 위한 데이터 모델
 */
@Parcelize
data class ProjectData(
    val id: Long,               // 프로젝트 고유 ID
    val title: String,          // 프로젝트 제목
    val category: String,       // 카테고리
    val yarnAndNeedle: String,  // 실 및 바늘 정보
    val currentRows: Int,       // 현재 진행 단수
    val totalRows: Int,         // 목표 전체 단수
    val status: String,         // 진행 상태
    val memo: String            // 메모 내용
) : Parcelable