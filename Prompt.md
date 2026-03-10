# Claude Code Prompt: Android Staggered Grid App

## Project Setup

Create a new Android application using the latest stable Android public SDK and Gradle version. The app must use **Kotlin** exclusively and implement its entire UI in **Jetpack Compose**. Use Material 3 for theming. Target the latest stable `compileSdk` and set `minSdk` to 26. Use version catalog (`libs.versions.toml`) for dependency management.

The project should follow a clean package structure:

```
com.example.staggeredgrid/
├── data/
│   ├── model/
│   │   └── GridItem.kt
│   ├── repository/
│   │   └── GridItemRepository.kt
│   └── generator/
│       └── DatasetGenerator.kt
├── ui/
│   ├── theme/
│   ├── components/
│   │   ├── GridItemCard.kt
│   │   └── NetworkSpeedControls.kt
│   ├── screen/
│   │   └── StaggeredGridScreen.kt
│   └── viewmodel/
│       └── GridViewModel.kt
└── MainActivity.kt
```

---

## Data Model

Define a data class `GridItem` with the following properties:

```kotlin
data class GridItem(
    val id: String,           // Format: "item_1", "item_2", ... "item_N"
    val title: String,        // Human-readable title
    val icon: String,         // Material icon name (used with Compose Material Icons)
    val heightDp: Int,        // Random value between 40 and 150 inclusive
    val spanType: SpanType    // SINGLE or FULL
)

enum class SpanType {
    SINGLE,  // Occupies one column/lane
    FULL     // Spans across both columns/lanes
}
```

---

## Dataset Generation

Create a `DatasetGenerator` object that produces a configurable number of items (default: **100**). The count should be defined as a constant that's easy to change:

```kotlin
object DatasetGenerator {
    const val DEFAULT_DATASET_SIZE = 100
    // ...
}
```

### Requirements:

- Each item gets a unique sequential ID: `"item_1"`, `"item_2"`, ..., `"item_100"`.
- **Heights**: Randomly assigned between 40dp and 150dp (inclusive) per item.
- **Span types**: Roughly 70% SINGLE, 30% FULL — assigned randomly per item.
- **Titles**: Draw from a diverse pool of at least 25 distinct titles. Examples:
  - "Sunset Horizon", "Morning Brew", "Deep Focus", "Cloud Atlas", "Iron Forge",
    "Pixel Art", "Night Owl", "Green Thumb", "Blue Ocean", "Red Planet",
    "Golden Hour", "Silver Lining", "Dark Matter", "Light Speed", "Quantum Leap",
    "Coral Reef", "Arctic Wind", "Desert Rose", "Thunder Peak", "Crystal Lake",
    "Velvet Sky", "Neon Pulse", "Amber Glow", "Jade Temple", "Obsidian Gate"
- **Icons**: Draw from a pool of at least 15 distinct Material Icons available in `androidx.compose.material.icons.Icons.Rounded` (or `.Filled`). Examples:
  - `Icons.Rounded.Star`, `Icons.Rounded.Favorite`, `Icons.Rounded.Bolt`,
    `Icons.Rounded.Waves`, `Icons.Rounded.Park`, `Icons.Rounded.Cloud`,
    `Icons.Rounded.Terrain`, `Icons.Rounded.Diamond`, `Icons.Rounded.Palette`,
    `Icons.Rounded.MusicNote`, `Icons.Rounded.Rocket`, `Icons.Rounded.Anchor`,
    `Icons.Rounded.LocalFireDepartment`, `Icons.Rounded.AutoAwesome`, `Icons.Rounded.Explore`
- Use a seeded `Random` instance so the dataset is reproducible across runs for easier debugging.

---

## Repository / Data Loading with Simulated Network Delay

Create a `GridItemRepository` that serves as the data source. It must support **paginated loading** of 20 items at a time with a **simulated network delay**.

### Network Speed Simulation:

Define an enum for network speed:

```kotlin
enum class NetworkSpeed(val label: String, val delayRange: LongRange) {
    FAST("Fast (200-500ms)", 200L..500L),
    SLOW("Slow (2000-4000ms)", 2_000L..4_000L)
}
```

### Repository behavior:

- Internally holds the full generated dataset.
- Exposes a `suspend fun loadPage(page: Int, pageSize: Int = 20, speed: NetworkSpeed): List<GridItem>` method.
- On each call, apply a `delay()` using a random value within the selected `NetworkSpeed.delayRange` to simulate latency.
- Return the appropriate slice of items, or an empty list if the page is beyond the dataset.
- The repository should also expose `val totalItems: Int` so the UI knows when all items have been loaded.

---

## ViewModel

Create a `GridViewModel` using `androidx.lifecycle.ViewModel` and `kotlinx.coroutines`:

### State:

```kotlin
data class GridUiState(
    val items: List<GridItem> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 0,
    val hasMoreItems: Boolean = true,
    val networkSpeed: NetworkSpeed = NetworkSpeed.FAST,
    val loadTimeMs: Long? = null  // Time taken for the last page load
)
```

### Behavior:

- On initialization, automatically load the first page.
- Expose a `fun loadNextPage()` method that loads the next batch of 20 items, appending them to the existing list.
- Expose a `fun setNetworkSpeed(speed: NetworkSpeed)` method. Changing speed should NOT clear already-loaded items — it only affects future loads.
- Track and expose the time each page load takes (wall-clock time of the suspend call) so the UI can display it.
- Prevent duplicate concurrent page loads (guard against rapid-fire triggers).

---

## UI: Staggered Grid Screen

Build a single-screen app with the following Compose layout:

### Top Section — Network Speed Controls:

- A `Row` or small control bar at the top with:
  - Two `FilterChip` or `SegmentedButton` components to toggle between **Fast** and **Slow** network speed.
  - A `Text` showing the last load duration, e.g., "Last load: 342ms".
  - A small `Text` showing the item count, e.g., "48 / 100 items".

### Main Content — `LazyVerticalStaggeredGrid`:

Use `androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid` with these specifications:

- **Columns**: `StaggeredGridCells.Fixed(2)` — always 2 columns.
- **Content padding**: 8dp on all sides.
- **Horizontal and vertical arrangement spacing**: 8dp.

#### Item rendering:

For each `GridItem`, render a `Card` (Material 3) with:

- **Height**: Set to the item's `heightDp` value using `Modifier.height(item.heightDp.dp)`.
- **Span**: 
  - `SINGLE` → `StaggeredGridItemSpan.SingleLane`
  - `FULL` → `StaggeredGridItemSpan.FullLine`
- **Card content** (arranged in a `Column` with padding):
  - **ID label**: Small text at the top, styled with `MaterialTheme.typography.labelSmall` and `color = MaterialTheme.colorScheme.onSurfaceVariant`. Display the `id` field (e.g., "item_42").
  - **Icon**: The corresponding Material Icon, sized 24dp, tinted with `MaterialTheme.colorScheme.primary`.
  - **Title**: `MaterialTheme.typography.titleSmall`, max 2 lines, overflow ellipsis.
- **Card styling**:
  - FULL span items should have a visually distinct background — use `MaterialTheme.colorScheme.primaryContainer` for full-span and `MaterialTheme.colorScheme.surfaceVariant` for single-span.
  - Subtle elevation (1-2dp for single, 3-4dp for full).
  - Rounded corners (12dp).

#### Pagination trigger:

- Detect when the user has scrolled near the end of the currently loaded items.
- Use `LazyStaggeredGridState` and a `LaunchedEffect` or `snapshotFlow` on the layout info to determine when the last visible item index is within 5 items of the end of the current list.
- When triggered, call `viewModel.loadNextPage()`.

#### Loading indicator:

- When `isLoading` is true and there are more items to load, show a full-width `item` at the bottom of the grid with a `CircularProgressIndicator` centered inside it. This item should use `StaggeredGridItemSpan.FullLine`.

#### End-of-list indicator:

- When all items are loaded (`hasMoreItems == false`), show a full-line item with a text like "All 100 items loaded ✓".

---

## Additional Requirements

1. **No navigation library needed** — this is a single-screen app.
2. **No database or network library needed** — all data is generated in-memory with `kotlinx.coroutines.delay` simulating latency.
3. **Use `collectAsStateWithLifecycle()`** from `androidx.lifecycle.compose` for observing state in Compose.
4. **Ensure the app handles configuration changes** (screen rotation) without losing loaded data — the ViewModel handles this naturally.
5. **Add smooth item appearance animations**: Use `Modifier.animateItem()` on each grid item for a fade+slide animation as items appear.
6. **Edge case**: If the dataset size is not evenly divisible by the page size, the last page should return whatever items remain.

---

## Build & Run

- The project must compile and run without errors on the latest stable Android Studio.
- Target a Pixel 7 emulator or equivalent (API 34+).
- Use Gradle Kotlin DSL (`build.gradle.kts`).

