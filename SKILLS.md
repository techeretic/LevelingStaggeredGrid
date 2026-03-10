# SKILLS.md — Techniques & Patterns Used

A log of the significant engineering skills and patterns applied in this project, useful as a reference for similar problems.

---

## LazyVerticalStaggeredGrid Layout

**What:** Jetpack Compose's `LazyVerticalStaggeredGrid` with `StaggeredGridCells.Fixed(2)` renders a 2-column grid where each column advances independently, creating a natural staggered/Pinterest-style layout.

**Span control:**
```kotlin
span = { item ->
    when (item.spanType) {
        SpanType.SINGLE -> StaggeredGridItemSpan.SingleLane
        SpanType.FULL   -> StaggeredGridItemSpan.FullLine
    }
}
```
FULL-span items force both columns to the same height (the taller column's current bottom), leaving a gap in the shorter column. Minimising that gap is the core challenge this project addresses.

**Key parameters used:**
- `verticalItemSpacing = 4.dp` — minimum vertical gap between all items
- `contentPadding = PaddingValues(8.dp)` — outer padding
- `horizontalArrangement = Arrangement.spacedBy(8.dp)` — column gutter

---

## Text-as-Height Proxy

**Problem:** Real-world API items often don't include a `heightDp` field. Heights must be inferred from content.

**Technique:** Description text length drives card height. Longer description → more wrapped lines → taller card. The optimizer estimates height as:
```
height ≈ BASE_HEIGHT + LINE_HEIGHT × ceil(description.length / CHARS_PER_LINE)
```
`BASE_HEIGHT = 88dp` (derived from card anatomy: label + icon + title + padding).
`LINE_HEIGHT = 16dp` (bodySmall line height). `CHARS_PER_LINE = 23` (empirical for half-screen cards).

This means the data model carries no explicit height — text content *is* the height.

---

## Column-Balancing via Hill-Climbing (SegmentOptimizer)

**Problem:** Given a list of items with estimated heights, find the ordering that minimises the column height gap at the end of each segment (just before a FULL-span item).

**Algorithm:**
1. **Segmentation** — Split at FULL-span boundaries; FULL items are never reordered.
2. **LPT initial solution** — Sort descending by height (Longest Processing Time first), a classical 2-machine scheduling heuristic.
3. **Hill-climbing with pairwise swaps** — Try swapping every pair (i, j); accept any swap that lowers the score. Repeat until no improvement.
4. **Scoring** — Simulate greedy 2-column assignment (`colA ≤ colB → item goes to colA`), return final `|colA − colB|`.

**Critical insight — objective function:** The score must measure *final* imbalance only, not cumulative. A cumulative score optimises for visual raggedness throughout the segment but ignores the actual gap at the FULL boundary. Final-only directly targets the visible artefact.

**Starting heights:** Each segment's simulation begins with the column heights left by the previous segment, not zero. FULL items reset both to `max(colA, colB) + fullH`.

---

## Greedy 2-Column Simulation

Used in both `SegmentOptimizer` (scoring) and `GridViewModel` (cross-page column tracking):
```kotlin
for (item in items) {
    val h = estimateHeight(item, measuredHeights)
    if (colA <= colB) colA += h else colB += h
}
```
This mirrors `LazyVerticalStaggeredGrid`'s internal lane-selection policy exactly, making the simulation reliable.

---

## Measured Height Feedback Loop

**Problem:** Height estimation from text length is approximate. Actual rendered heights depend on font metrics, line wrapping, and device density.

**Technique:** Each rendered card reports its actual height back to the ViewModel via a callback:

```kotlin
// GridItemCard.kt
Modifier.onSizeChanged { size ->
    val heightDp = with(density) { size.height.toDp().value.toInt() }
    onHeightMeasured(item.id, heightDp)
}
```

`GridViewModel` stores `measuredHeights: Map<String, Int>` in `GridUiState`. When the next page is loaded, these measured heights are passed to `SegmentOptimizer`, which prefers them over formula estimates.

**Rule:** Measured heights only influence *future* page optimisations — already-rendered items are never reordered to avoid visual disruption.

---

## Cross-Page Column Awareness

**Problem:** With paginated loading, each new page of items is inserted into a grid where the two columns may already be at different heights. Optimising the page assuming `(0, 0)` starting heights produces suboptimal orderings.

**Technique:** Before loading page N, `GridViewModel.computeColumnHeights()` simulates the greedy assignment over all already-loaded items (using measured heights where available) to derive the actual `(colA, colB)` at the current scroll bottom. These are passed to `GridItemRepository.loadPage()` → `SegmentOptimizer.optimize()` as `startColA / startColB`.

---

## Pagination with `derivedStateOf`

Scroll-triggered pagination without unnecessary recomposition:
```kotlin
val shouldLoadMore by remember {
    derivedStateOf {
        val last  = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val total = gridState.layoutInfo.totalItemsCount
        last >= total - 5
    }
}
LaunchedEffect(shouldLoadMore) {
    if (shouldLoadMore) viewModel.loadNextPage()
}
```
`derivedStateOf` ensures the boolean only changes when it actually flips, preventing rapid-fire load calls as the user scrolls through the threshold.

---

## Simulated Network Delay with `delay`

```kotlin
enum class NetworkSpeed(val label: String, val delayRange: LongRange) {
    FAST("Fast (200-500ms)",   200L..500L),
    SLOW("Slow (2000-4000ms)", 2_000L..4_000L)
}
val delayMs = Random.nextLong(speed.delayRange.first, speed.delayRange.last + 1)
delay(delayMs)
```
Simulates realistic network jitter without blocking threads. Useful for testing loading states and animations.

---

## Seeded Randomisation for Reproducible Datasets

```kotlin
val random = Random(seed = 42L)
```
Using a fixed seed guarantees the same 1000 items (same titles, icons, descriptions, span types) across every app run and every device. Invaluable for debugging layout issues — the same gap appears in the same position every time.

---

## Build Toolchain: Pinning Gradle JDK via `gradle.properties`

**Problem:** System JDK (25.0.2) is incompatible with Gradle 8.11.1's embedded Kotlin DSL compiler (`JavaVersion.parse` throws `IllegalArgumentException` on 3-part EA/LTS version strings like "25.0.2").

**Solution:** Pin the Gradle daemon to a compatible JDK without changing the system default:
```properties
# gradle.properties
org.gradle.java.home=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```
The app bytecode still targets JVM 11 via `compileOptions { sourceCompatibility = JavaVersion.VERSION_11 }`.
