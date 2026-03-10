package com.pshetye.staggeredgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import com.pshetye.staggeredgrid.data.repository.GridItemRepository
import com.pshetye.staggeredgrid.data.repository.NetworkSpeed
import com.pshetye.staggeredgrid.leveling.StaggeredGridLeveler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

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

    private val leveler = StaggeredGridLeveler<GridItem>(
        keySelector = { it.id },
        isFullSpan = { it.spanType == SpanType.FULL },
        estimateHeight = { item ->
            val descriptionLines = ceil(item.description.length.toDouble() / CHARS_PER_LINE).toInt()
            BASE_HEIGHT + LINE_HEIGHT * descriptionLines
        }
    )

    private val repository = GridItemRepository(leveler)

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
            val (colA, colB) = leveler.computeColumnHeights(
                currentState.items, currentState.measuredHeights
            )

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

    fun onItemHeightMeasured(itemId: String, heightDp: Int) {
        val current = _uiState.value
        if (current.measuredHeights[itemId] == heightDp) return
        _uiState.update { it.copy(measuredHeights = it.measuredHeights + (itemId to heightDp)) }
    }

    companion object {
        private const val BASE_HEIGHT = 88
        private const val LINE_HEIGHT = 16
        private const val CHARS_PER_LINE = 23
    }
}
