package com.konit.hankovillage.ui.yarn

/**
 * 실 데이터 클래스
 */
data class YarnData(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var color: String,
    var shop: String = "",
    var purchaseDate: String = "",
    var memo: String = "",
    var imageUrl: String? = null
) {
    init {
        if (name.length > 50) {
            name = name.take(50)
        }
        if (color.length > 30) {
            color = color.take(30)
        }
        if (shop.length > 100) {
            shop = shop.take(100)
        }
        if (memo.length > 100) {
            memo = memo.take(100)
        }
    }
}