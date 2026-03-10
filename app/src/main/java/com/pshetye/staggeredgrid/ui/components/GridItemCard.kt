package com.pshetye.staggeredgrid.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType

@Composable
fun GridItemCard(
    item: GridItem,
    modifier: Modifier = Modifier,
    onHeightMeasured: ((itemId: String, heightDp: Int) -> Unit)? = null
) {
    val isFullSpan = item.spanType == SpanType.FULL
    val containerColor = if (isFullSpan) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val elevation = if (isFullSpan) 3.dp else 1.dp
    val density = LocalDensity.current

    Card(
        modifier = modifier.then(
            if (onHeightMeasured != null) {
                Modifier.onSizeChanged { size ->
                    val heightDp = with(density) { size.height.toDp().value.toInt() }
                    onHeightMeasured(item.id, heightDp)
                }
            } else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = item.id,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = iconFromName(item.icon),
                contentDescription = item.icon,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun iconFromName(name: String): ImageVector = when (name) {
    "Star" -> Icons.Rounded.Star
    "Favorite" -> Icons.Rounded.Favorite
    "Bolt" -> Icons.Rounded.Bolt
    "Waves" -> Icons.Rounded.Waves
    "Park" -> Icons.Rounded.Park
    "Cloud" -> Icons.Rounded.Cloud
    "Terrain" -> Icons.Rounded.Terrain
    "Diamond" -> Icons.Rounded.Diamond
    "Palette" -> Icons.Rounded.Palette
    "MusicNote" -> Icons.Rounded.MusicNote
    "Anchor" -> Icons.Rounded.Anchor
    "LocalFireDepartment" -> Icons.Rounded.LocalFireDepartment
    "AutoAwesome" -> Icons.Rounded.AutoAwesome
    "Explore" -> Icons.Rounded.Explore
    "Spa" -> Icons.Rounded.Spa
    else -> Icons.Rounded.Star
}
