package com.example.runway.ui.navigation

object Routes {
    const val ITEMS = "items"

    const val ARG_ITEM_ID = "itemId"
    const val ITEM_DETAIL = "items/{itemId}"

    fun itemDetail(id: Long): String = "items/$id"
}
