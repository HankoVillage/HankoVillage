package com.konit.hankovillage

// 메모(memo) 속성 추가
data class ProjectData(
    val id: Long = System.currentTimeMillis(), // 고유 ID
    var title: String,         // 작품 이름
    var needleType: String,    // 바늘 종류 (예: 대바늘 / 코바늘)
    var info: String,          // 실 및 바늘 정보
    var currentCount: Int,     // 현재 진행 단수
    var targetCount: Int,      // 목표 단수
    var status: String = "진행", // 진행 상태
    var memo: String = ""      // 뜨개 메모
)