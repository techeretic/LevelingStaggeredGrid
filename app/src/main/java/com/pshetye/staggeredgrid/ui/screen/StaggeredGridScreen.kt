package com.pshetye.staggeredgrid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pshetye.staggeredgrid.data.generator.DatasetGenerator
import com.pshetye.staggeredgrid.data.model.SpanType
import com.pshetye.staggeredgrid.ui.components.GridItemCard
import com.pshetye.staggeredgrid.ui.components.NetworkSpeedControls
import com.pshetye.staggeredgrid.ui.viewmodel.GridViewModel

@Composable
fun StaggeredGridScreen(
    viewModel: GridViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItemsCount = layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextPage()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        NetworkSpeedControls(
            selectedSpeed = uiState.networkSpeed,
            loadTimeMs = uiState.loadTimeMs,
            itemCount = uiState.items.size,
            totalItems = DatasetGenerator.DEFAULT_DATASET_SIZE,
            onSpeedSelected = { viewModel.setNetworkSpeed(it) }
        )

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = uiState.items,
                key = { it.id },
                span = { item ->
                    when (item.spanType) {
                        SpanType.SINGLE -> StaggeredGridItemSpan.SingleLane
                        SpanType.FULL -> StaggeredGridItemSpan.FullLine
                    }
                }
            ) { item ->
                GridItemCard(
                    item = item,
                    modifier = Modifier.animateItem()
                )
            }

            if (uiState.isLoading && uiState.hasMoreItems) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!uiState.hasMoreItems) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All ${DatasetGenerator.DEFAULT_DATASET_SIZE} items loaded \u2713",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
