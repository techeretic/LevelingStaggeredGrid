package com.pshetye.staggeredgrid.data.model

data class GridItem(
    val id: String,
    val title: String,
    val icon: String,
    val description: String,
    val spanType: SpanType
)

enum class SpanType {
    SINGLE,
    FULL
}
