package com.pshetye.staggeredgrid.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pshetye.staggeredgrid.data.model.GridItem
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
    val loadTimeMs: Long? = null
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

            val startTime = System.currentTimeMillis()
            val newItems = repository.loadPage(
                page = state.currentPage,
                speed = state.networkSpeed
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
}
