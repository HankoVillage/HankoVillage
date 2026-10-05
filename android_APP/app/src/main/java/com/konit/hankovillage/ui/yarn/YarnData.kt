package com.konit.hankovillage.ui.yarn

data class YarnData(
    val id: Long = System.currentTimeMillis(),
    var name: String,              // 실 이름
    var color: String,             // 색상
    var shop: String = "",         // 구매처
    var purchaseDate: String = "", // 구매일
    var memo: String = "",         // 메모
    var imageUri: String? = null   // 사진 (카메라/앨범 선택 URI)
)