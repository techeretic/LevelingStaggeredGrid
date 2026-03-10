package com.pshetye.staggeredgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pshetye.staggeredgrid.data.generator.SegmentOptimizer
import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import com.pshetye.staggeredgrid.data.repository.GridItemRepository
import com.pshetye.staggeredgrid.data.repository.NetworkSpeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GridUiState(
    val items: List<GridItem> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val hasMoreItems: Boolean = true,
    val networkSpeed: NetworkSpeed = NetworkSpeed.FAST,
    val loadTimeMs: Long? = null,
    val measuredHeights: Map<String, Int> = emptyMap()
)

class GridViewModel : ViewModel() {

    private val repository = GridItemRepository()

    private val _uiState = MutableStateFlow(GridUiState())
    val uiState: StateFlow<GridUiState> = _uiState.asStateFlow()

    init {
        loadNextPage()
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || !state.hasMoreItems) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentState = _uiState.value
            val (colA, colB) = computeColumnHeights(currentState.items, currentState.measuredHeights)

            val startTime = System.currentTimeMillis()
            val newItems = repository.loadPage(
                page = currentState.currentPage,
                speed = currentState.networkSpeed,
                measuredHeights = currentState.measuredHeights,
                startColA = colA,
                startColB = colB
            )
            val loadTime = System.currentTimeMillis() - startTime

            _uiState.update { current ->
                current.copy(
                    items = current.items + newItems,
                    isLoading = false,
                    currentPage = current.currentPage + 1,
                    hasMoreItems = current.items.size + newItems.size < repository.totalItems,
                    loadTimeMs = loadTime
                )
            }
        }
    }

    fun setNetworkSpeed(speed: NetworkSpeed) {
        _uiState.update { it.copy(networkSpeed = speed) }
    }

    /** Called by the UI when a card's actual rendered height is known. */
    fun onItemHeightMeasured(itemId: String, heightDp: Int) {
        val current = _uiState.value
        if (current.measuredHeights[itemId] == heightDp) return
        _uiState.update { it.copy(measuredHeights = it.measuredHeights + (itemId to heightDp)) }
    }

    /**
     * Simulates the greedy 2-column assignment for already-loaded items to determine
     * the current column heights. FULL-span items level both columns; SINGLE items go
     * to the shorter column (mirroring LazyVerticalStaggeredGrid's internal logic).
     */
    private fun computeColumnHeights(
        items: List<GridItem>,
        measuredHeights: Map<String, Int>
    ): Pair<Int, Int> {
        var colA = 0
        var colB = 0
        for (item in items) {
            val h = SegmentOptimizer.estimateHeight(item, measuredHeights)
            when (item.spanType) {
                SpanType.FULL -> {
                    val level = maxOf(colA, colB) + h
                    colA = level
                    colB = level
                }
                SpanType.SINGLE -> {
                    if (colA <= colB) colA += h else colB += h
                }
            }
        }
        return Pair(colA, colB)
    }
}
