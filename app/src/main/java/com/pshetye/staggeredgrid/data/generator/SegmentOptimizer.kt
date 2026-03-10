package com.pshetye.staggeredgrid.data.generator

import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import kotlin.math.abs
import kotlin.math.ceil

object SegmentOptimizer {

    // Approximate base height: ID label + icon + spacers + title (1 line) + padding
    // labelSmall(16) + spacer(4) + icon(24) + spacer(4) + titleSmall(20) + spacer(4) + padding(8+8) = 88dp
    private const val BASE_HEIGHT = 88
    // bodySmall line height ~16sp
    private const val LINE_HEIGHT = 16
    // Characters per line in a half-screen-width card (conservative estimate for smaller screens)
    private const val CHARS_PER_LINE = 23

    /**
     * Returns the best known height for an item: measured actual height when available,
     * otherwise estimated from text content length.
     */
    fun estimateHeight(item: GridItem, measuredHeights: Map<String, Int> = emptyMap()): Int {
        return measuredHeights[item.id] ?: run {
            val descriptionLines = ceil(item.description.length.toDouble() / CHARS_PER_LINE).toInt()
            BASE_HEIGHT + LINE_HEIGHT * descriptionLines
        }
    }

    /**
     * Reorders [items] to minimize column height imbalance at each FULL-span boundary.
     *
     * @param measuredHeights actual rendered heights (item id → height in dp) collected from the UI
     * @param startColA current height of column A before this batch of items
     * @param startColB current height of column B before this batch of items
     */
    fun optimize(
        items: List<GridItem>,
        measuredHeights: Map<String, Int> = emptyMap(),
        startColA: Int = 0,
        startColB: Int = 0
    ): List<GridItem> {
        val result = mutableListOf<GridItem>()
        val currentSegment = mutableListOf<GridItem>()
        var colA = startColA
        var colB = startColB

        for (item in items) {
            if (item.spanType == SpanType.FULL) {
                // Optimize the accumulated SINGLE-span segment before this FULL item
                val optimized = optimizeSegment(currentSegment, measuredHeights, colA, colB)
                result.addAll(optimized)
                currentSegment.clear()

                // Advance column heights to reflect the optimized segment
                val (newColA, newColB) = simulateColumns(optimized, measuredHeights, colA, colB)
                // FULL item levels both columns to the taller one + its own height
                val fullH = estimateHeight(item, measuredHeights)
                val levelHeight = maxOf(newColA, newColB) + fullH
                colA = levelHeight
                colB = levelHeight

                result.add(item)
            } else {
                currentSegment.add(item)
            }
        }

        // Optimize the trailing segment (no FULL item follows)
        result.addAll(optimizeSegment(currentSegment, measuredHeights, colA, colB))
        return result
    }

    /** Simulates greedy 2-column assignment and returns resulting column heights. */
    fun simulateColumns(
        items: List<GridItem>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): Pair<Int, Int> {
        var colA = startColA
        var colB = startColB
        for (item in items) {
            val h = estimateHeight(item, measuredHeights)
            if (colA <= colB) colA += h else colB += h
        }
        return Pair(colA, colB)
    }

    private fun optimizeSegment(
        segment: List<GridItem>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): List<GridItem> {
        if (segment.size <= 1) return segment
        if (segment.size == 2) return segment.sortedByDescending { estimateHeight(it, measuredHeights) }

        // Start with LPT (tallest first) — known to be a strong 2-machine heuristic
        var best = segment.sortedByDescending { estimateHeight(it, measuredHeights) }
        var bestScore = gapScore(best, measuredHeights, startColA, startColB)

        // Hill climbing: try all pairwise swaps, keep the best improvement
        var improved = true
        while (improved) {
            improved = false
            for (i in 0 until best.size - 1) {
                for (j in i + 1 until best.size) {
                    val candidate = best.toMutableList()
                    candidate[i] = best[j]
                    candidate[j] = best[i]
                    val score = gapScore(candidate, measuredHeights, startColA, startColB)
                    if (score < bestScore) {
                        best = candidate
                        bestScore = score
                        improved = true
                    }
                }
            }
        }

        return best
    }

    /**
     * Scores an ordering by the FINAL column height imbalance only.
     *
     * The previous implementation accumulated imbalance at every step, which optimized
     * for visual raggedness rather than the gap that actually appears at FULL-span boundaries.
     * Only the final |colA - colB| determines how large that gap will be.
     */
    private fun gapScore(
        items: List<GridItem>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): Long {
        var colA = startColA
        var colB = startColB
        for (item in items) {
            val h = estimateHeight(item, measuredHeights)
            if (colA <= colB) colA += h else colB += h
        }
        return abs(colA - colB).toLong()
    }
}
