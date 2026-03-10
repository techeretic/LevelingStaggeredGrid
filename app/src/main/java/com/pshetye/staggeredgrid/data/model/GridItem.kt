package com.pshetye.staggeredgrid.data.model

data class GridItem(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val spanType: SpanType,
    val color: Long
)

enum class SpanType {
    SINGLE,
    FULL
}
