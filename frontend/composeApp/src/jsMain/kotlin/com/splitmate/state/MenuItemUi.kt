package com.splitmate.state

data class MenuItemUi(
    val id: Int,
    val name: String = "",
    val priceInput: String = "",
    val nameError: String? = null,
    val priceError: String? = null,
)