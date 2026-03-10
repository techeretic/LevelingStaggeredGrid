package com.pshetye.staggeredgrid.data.generator

import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import kotlin.random.Random

object DatasetGenerator {

    const val DEFAULT_DATASET_SIZE = 100

    private val titles = listOf(
        "Sunset Horizon", "Morning Brew", "Deep Focus", "Cloud Atlas", "Iron Forge",
        "Pixel Art", "Night Owl", "Green Thumb", "Blue Ocean", "Red Planet",
        "Golden Hour", "Silver Lining", "Dark Matter", "Light Speed", "Quantum Leap",
        "Coral Reef", "Arctic Wind", "Desert Rose", "Thunder Peak", "Crystal Lake",
        "Velvet Sky", "Neon Pulse", "Amber Glow", "Jade Temple", "Obsidian Gate"
    )

    private val icons = listOf(
        "Star", "Favorite", "Bolt", "Waves", "Park",
        "Cloud", "Terrain", "Diamond", "Palette", "MusicNote",
        "Anchor", "LocalFireDepartment", "AutoAwesome", "Explore", "Spa"
    )

    fun generate(count: Int = DEFAULT_DATASET_SIZE, seed: Long = 42L): List<GridItem> {
        val random = Random(seed)
        var singleSinceLastFull = 5
        return (1..count).map { index ->
            val spanType = if (singleSinceLastFull < 5 || random.nextFloat() >= 0.3f) {
                SpanType.SINGLE
            } else {
                SpanType.FULL
            }
            singleSinceLastFull = if (spanType == SpanType.FULL) 0 else singleSinceLastFull + 1
            GridItem(
                id = "item_$index",
                title = titles[random.nextInt(titles.size)],
                icon = icons[random.nextInt(icons.size)],
                heightDp = random.nextInt(120, 301),
                spanType = spanType
            )
        }
    }
}
