package com.pshetye.staggeredgrid.data.repository

import com.pshetye.staggeredgrid.data.generator.DatasetGenerator
import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.leveling.StaggeredGridLeveler
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class NetworkSpeed(val label: String, val delayRange: LongRange) {
    FAST("Fast (200-500ms)", 200L..500L),
    SLOW("Slow (2000-4000ms)", 2_000L..4_000L)
}

class GridItemRepository(private val leveler: StaggeredGridLeveler<GridItem>) {

    private val rawDataset: List<GridItem> = DatasetGenerator.generate()

    val totalItems: Int get() = rawDataset.size

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

        return leveler.optimize(pageItems, measuredHeights, startColA, startColB)
    }
}
