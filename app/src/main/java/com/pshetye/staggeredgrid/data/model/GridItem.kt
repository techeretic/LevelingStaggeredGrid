package com.pshetye.staggeredgrid.data.model

data class GridItem(
    val id: String,
    val title: String,
    val icon: String,
    val heightDp: Int,
    val spanType: SpanType
)

enum class SpanType {
    SINGLE,
    FULL
}
