package com.pshetye.staggeredgrid.data.generator

import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import kotlin.math.abs

object SegmentOptimizer {

    fun optimize(items: List<GridItem>): List<GridItem> {
        val result = mutableListOf<GridItem>()
        val currentSegment = mutableListOf<GridItem>()

        for (item in items) {
            if (item.spanType == SpanType.FULL) {
                result.addAll(optimizeSegment(currentSegment))
                currentSegment.clear()
                result.add(item)
            } else {
                currentSegment.add(item)
            }
        }

        result.addAll(optimizeSegment(currentSegment))

        return result
    }

    private fun optimizeSegment(segment: List<GridItem>): List<GridItem> {
        if (segment.size <= 1) return segment
        if (segment.size == 2) return segment.sortedByDescending { it.heightDp }

        // Start with LPT (descending sort) as initial solution
        var best = segment.sortedByDescending { it.heightDp }
        var bestScore = gapScore(best)

        // Hill climbing: try all pairwise swaps, keep improving
        var improved = true
        while (improved) {
            improved = false
            for (i in 0 until best.size - 1) {
                for (j in i + 1 until best.size) {
                    val candidate = best.toMutableList()
                    candidate[i] = best[j]
                    candidate[j] = best[i]
                    val score = gapScore(candidate)
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
     * Simulates the grid's "shortest column first" placement and returns
     * a gap score. Lower is better — columns stay more balanced.
     */
    private fun gapScore(items: List<GridItem>): Long {
        var colA = 0
        var colB = 0
        var score = 0L

        for (item in items) {
            if (colA <= colB) {
                colA += item.heightDp
            } else {
                colB += item.heightDp
            }
            score += abs(colA - colB)
        }

        // Extra penalty for final imbalance (affects next full-span item)
        score += abs(colA - colB)

        return score
    }
}
