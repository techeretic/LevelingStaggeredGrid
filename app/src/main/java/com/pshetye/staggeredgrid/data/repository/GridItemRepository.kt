package com.pshetye.staggeredgrid.data.repository

import com.pshetye.staggeredgrid.data.generator.DatasetGenerator
import com.pshetye.staggeredgrid.data.generator.SegmentOptimizer
import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class NetworkSpeed(val label: String, val delayRange: LongRange) {
    FAST("Fast (200-500ms)", 200L..500L),
    SLOW("Slow (2000-4000ms)", 2_000L..4_000L)
}

class GridItemRepository {

    // Raw, unordered dataset — ordering is determined per page load using measured heights
    private val rawDataset: List<GridItem> = DatasetGenerator.generate()

    val totalItems: Int get() = rawDataset.size

    /**
     * Loads and optimizes a page of items.
     *
     * @param measuredHeights actual rendered heights (item id → dp) from the UI
     * @param startColA current height of column A at the point these items will be inserted
     * @param startColB current height of column B at the point these items will be inserted
     */
    suspend fun loadPage(
        page: Int,
        pageSize: Int = 20,
        speed: NetworkSpeed,
        measuredHeights: Map<String, Int> = emptyMap(),
        startColA: Int = 0,
        startColB: Int = 0
    ): List<GridItem> {
        val delayMs = Random.nextLong(speed.delayRange.first, speed.delayRange.last + 1)
        delay(delayMs)

        val startIndex = page * pageSize
        if (startIndex >= rawDataset.size) return emptyList()

        val endIndex = minOf(startIndex + pageSize, rawDataset.size)
        val pageItems = rawDataset.subList(startIndex, endIndex)

        return SegmentOptimizer.optimize(pageItems, measuredHeights, startColA, startColB)
    }
}
