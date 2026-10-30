package com.konit.hankovillage.ui.yarn

data class YarnData(
    val id: Long = System.currentTimeMillis(),
    var name: String,
    var color: String,
    var shop: String = "",
    var purchaseDate: String = "",
    var memo: String = "",
    var imageUrl: String? = null
)