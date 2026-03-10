package com.pshetye.staggeredgrid.leveling

import kotlin.math.abs

/**
 * Generic optimizer that reorders items in a `LazyVerticalStaggeredGrid` to minimise
 * column-height gaps at full-span boundaries.
 *
 * Works with any item type [T]. The caller supplies three lambdas that teach the
 * leveler how to interpret items:
 *
 * @param keySelector   Unique stable identifier for an item (used to look up measured heights).
 * @param isFullSpan    Returns `true` if the item spans all columns (like `StaggeredGridItemSpan.FullLine`).
 * @param estimateHeight Returns a best-effort height estimate (in dp or any consistent unit)
 *                       based on the item's content alone. This is used when no measured
 *                       height is available yet.
 *
 * Usage:
 * ```
 * val leveler = StaggeredGridLeveler<MyItem>(
 *     keySelector    = { it.id },
 *     isFullSpan     = { it.spanType == SpanType.FULL },
 *     estimateHeight = { BASE_HEIGHT + LINE_HEIGHT * ceil(it.text.length / CHARS_PER_LINE) }
 * )
 *
 * val optimized = leveler.optimize(pageItems, measuredHeights, colA, colB)
 * val (colA, colB) = leveler.computeColumnHeights(allItems, measuredHeights)
 * ```
 */
class StaggeredGridLeveler<T>(
    val keySelector: (T) -> String,
    val isFullSpan: (T) -> Boolean,
    val estimateHeight: (T) -> Int
) {

    /**
     * Returns the best known height for [item]: the measured actual height if present
     * in [measuredHeights], otherwise the content-based estimate.
     */
    fun resolveHeight(item: T, measuredHeights: Map<String, Int> = emptyMap()): Int {
        return measuredHeights[keySelector(item)] ?: estimateHeight(item)
    }

    /**
     * Reorders [items] to minimise column-height imbalance at each full-span boundary.
     *
     * Full-span items act as segment boundaries and are never reordered.
     * Single-span items between consecutive full-span items form a *segment*
     * whose internal order is optimised via LPT + hill-climbing.
     *
     * @param measuredHeights actual rendered heights (item key → height) collected from the UI
     * @param startColA current height of column A before this batch of items
     * @param startColB current height of column B before this batch of items
     */
    fun optimize(
        items: List<T>,
        measuredHeights: Map<String, Int> = emptyMap(),
        startColA: Int = 0,
        startColB: Int = 0
    ): List<T> {
        val result = mutableListOf<T>()
        val currentSegment = mutableListOf<T>()
        var colA = startColA
        var colB = startColB

        for (item in items) {
            if (isFullSpan(item)) {
                val optimized = optimizeSegment(currentSegment, measuredHeights, colA, colB)
                result.addAll(optimized)
                currentSegment.clear()

                val (newColA, newColB) = simulateSingleSpanColumns(optimized, measuredHeights, colA, colB)
                val fullH = resolveHeight(item, measuredHeights)
                val levelHeight = maxOf(newColA, newColB) + fullH
                colA = levelHeight
                colB = levelHeight

                result.add(item)
            } else {
                currentSegment.add(item)
            }
        }

        result.addAll(optimizeSegment(currentSegment, measuredHeights, colA, colB))
        return result
    }

    /**
     * Computes column heights after placing all [items], accounting for both
     * single-span (greedy shortest-column-first) and full-span (levels both columns)
     * assignment — mirroring `LazyVerticalStaggeredGrid`'s internal logic.
     */
    fun computeColumnHeights(
        items: List<T>,
        measuredHeights: Map<String, Int> = emptyMap(),
        startColA: Int = 0,
        startColB: Int = 0
    ): Pair<Int, Int> {
        var colA = startColA
        var colB = startColB
        for (item in items) {
            val h = resolveHeight(item, measuredHeights)
            if (isFullSpan(item)) {
                val level = maxOf(colA, colB) + h
                colA = level
                colB = level
            } else {
                if (colA <= colB) colA += h else colB += h
            }
        }
        return Pair(colA, colB)
    }

    /**
     * Simulates greedy 2-column assignment for single-span items only.
     * Used internally when computing segment column deltas.
     */
    private fun simulateSingleSpanColumns(
        items: List<T>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): Pair<Int, Int> {
        var colA = startColA
        var colB = startColB
        for (item in items) {
            val h = resolveHeight(item, measuredHeights)
            if (colA <= colB) colA += h else colB += h
        }
        return Pair(colA, colB)
    }

    private fun optimizeSegment(
        segment: List<T>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): List<T> {
        if (segment.size <= 1) return segment
        if (segment.size == 2) return segment.sortedByDescending { resolveHeight(it, measuredHeights) }

        var best = segment.sortedByDescending { resolveHeight(it, measuredHeights) }
        var bestScore = gapScore(best, measuredHeights, startColA, startColB)

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

    private fun gapScore(
        items: List<T>,
        measuredHeights: Map<String, Int>,
        startColA: Int,
        startColB: Int
    ): Long {
        var colA = startColA
        var colB = startColB
        for (item in items) {
            val h = resolveHeight(item, measuredHeights)
            if (colA <= colB) colA += h else colB += h
        }
        return abs(colA - colB).toLong()
    }
}
