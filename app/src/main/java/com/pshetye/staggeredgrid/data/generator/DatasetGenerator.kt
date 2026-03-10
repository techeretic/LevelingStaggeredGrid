package com.pshetye.staggeredgrid.data.generator

import com.pshetye.staggeredgrid.data.model.GridItem
import com.pshetye.staggeredgrid.data.model.SpanType
import kotlin.random.Random

object DatasetGenerator {

    const val DEFAULT_DATASET_SIZE = 1000

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

    private val sentences = listOf(
        "A warm glow spreads across the evening sky.",
        "The sound of waves fills the air with calm.",
        "Patterns emerge from the chaos of data.",
        "Tiny crystals form along the frozen edge.",
        "Light bends through the prism of thought.",
        "Roots dig deep beneath ancient stones.",
        "Signals travel faster than the eye can see.",
        "Colors shift as the temperature changes.",
        "The compass points toward unexplored terrain.",
        "Embers rise gently into the night above.",
        "A quiet hum resonates through the chamber.",
        "Layers of sediment reveal hidden stories.",
        "The algorithm converges on a stable solution.",
        "Starlight reflects off the surface of still water.",
        "Wind carries seeds across vast open plains.",
        "Pressure builds beneath the volcanic ridge.",
        "A single thread connects distant memories.",
        "The tide pulls back to reveal the shore.",
        "Frost patterns trace delicate lines on glass.",
        "Energy flows through the network of paths."
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

            val sentenceCount = random.nextInt(1, 6)
            val description = (1..sentenceCount)
                .map { sentences[random.nextInt(sentences.size)] }
                .joinToString(" ")

            val r = 0.55f + 0.35f * random.nextFloat()
            val g = 0.55f + 0.35f * random.nextFloat()
            val b = 0.55f + 0.35f * random.nextFloat()
            val argb = (0xFF shl 24) or
                ((r * 255).toInt() shl 16) or
                ((g * 255).toInt() shl 8) or
                (b * 255).toInt()

            GridItem(
                id = "item_$index",
                title = titles[random.nextInt(titles.size)],
                icon = icons[random.nextInt(icons.size)],
                description = description,
                spanType = spanType,
                color = argb.toLong() and 0xFFFFFFFFL
            )
        }
    }
}
