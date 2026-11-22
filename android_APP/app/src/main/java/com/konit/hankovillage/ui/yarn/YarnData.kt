package com.konit.hankovillage.ui.yarn

import com.google.gson.annotations.SerializedName

data class YarnData(
    @SerializedName("id")
    val id: Long = 0,

    @SerializedName("name")
    var name: String,                     // 필수 (1~50자)

    @SerializedName("color")
    var color: String? = null,            // 선택 (30자 이하, 서버 null 허용)

    @SerializedName("purchase_site")
    var shop: String? = null,             // 기존 shop -> 서버 purchase_site (100자 이하)

    @SerializedName("purchase_date")
    var purchaseDate: String? = null,     // 선택 (YYYY-MM-DD)

    @SerializedName("memo")
    var memo: String? = null,             // 선택 (100자 이하)

    @SerializedName("image_url")
    var imageUrl: String? = null,         // 상세 조회용 (/yarns/{id}/image)

    @SerializedName("thumbnail_url")
    var thumbnailUrl: String? = null      // 목록 썸네일용 (/yarns/{id}/image?thumb=true)
) {
    init {
        if (name.length > 50) {
            name = name.take(50)
        }
        if ((color?.length ?: 0) > 30) {
            color = color?.take(30)
        }
        if ((shop?.length ?: 0) > 100) {
            shop = shop?.take(100)
        }
        if ((memo?.length ?: 0) > 100) {
            memo = memo?.take(100)
        }
    }
}

/**
 * POST /yarns 등록 요청용 DTO
 */
data class YarnCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("color") val color: String? = null,
    @SerializedName("purchase_site") val purchaseSite: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("memo") val memo: String? = null
)

/**
 * PATCH /yarns/{id} 수정 요청용 DTO
 */
data class YarnUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("color") val color: String? = null,
    @SerializedName("purchase_site") val purchaseSite: String? = null,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("memo") val memo: String? = null
)