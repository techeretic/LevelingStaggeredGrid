package com.pshetye.staggeredgrid.data.repository

import com.pshetye.staggeredgrid.data.generator.DatasetGenerator
import com.pshetye.staggeredgrid.data.generator.SegmentOptimizer
import com.pshetye.staggeredgrid.data.model.GridItem
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class NetworkSpeed(val label: String, val delayRange: LongRange) {
    FAST("Fast (200-500ms)", 200L..500L),
    SLOW("Slow (2000-4000ms)", 2_000L..4_000L)
}

class GridItemRepository {

    private val dataset: List<GridItem> = SegmentOptimizer.optimize(DatasetGenerator.generate())

    val totalItems: Int get() = dataset.size

    suspend fun loadPage(page: Int, pageSize: Int = 20, speed: NetworkSpeed): List<GridItem> {
        val delayMs = Random.nextLong(speed.delayRange.first, speed.delayRange.last + 1)
        delay(delayMs)

        val startIndex = page * pageSize
        if (startIndex >= dataset.size) return emptyList()

        val endIndex = minOf(startIndex + pageSize, dataset.size)
        return dataset.subList(startIndex, endIndex)
    }
}
