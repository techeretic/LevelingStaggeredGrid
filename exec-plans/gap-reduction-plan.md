# Gap Reduction in LazyVerticalStaggeredGrid

## Problem Analysis

`LazyVerticalStaggeredGrid` places SINGLE-span items using a greedy shortest-column policy and FULL-span items across all lanes simultaneously. When a FULL item appears, it starts at `max(colA_height, colB_height)`, leaving a gap of `|colA_height - colB_height|` in the shorter column.

```
Column A: [item1][item3][item5]
Column B: [item2][item4]        ← shorter
FULL item:              ↑ gap here before FULL item spans both
```

Since we have no explicit `heightDp` field, item heights are determined entirely by their `description` text length. Longer descriptions → more wrapped lines → taller cards. This text-as-height relationship is both the constraint and the lever we optimize against.

## Root Causes of Current Gaps

1. **Wrong optimization objective** — `gapScore` accumulates `|colA - colB|` at every step, not just at the segment end. This optimizes for visual "raggedness" throughout the segment rather than the gap that actually appears at the FULL-span boundary.

2. **No cross-page column awareness** — The optimizer assumes both columns start at height 0 for every segment. In paginated loading, the columns may already be at different heights when a new page arrives.

3. **Height estimation inaccuracy** — `BASE_HEIGHT = 80` is lower than the actual card content (~88dp). Small errors compound across many items.

4. **Fixed spacing floor** — `verticalItemSpacing = 8.dp` adds a constant minimum gap between items that can't be reduced below that value.

## Approach: Per-Page Optimization with Measured Height Feedback

Since text content is the height proxy, we improve in three layers:

### Layer 1 — Fix the Optimizer Objective
Change `gapScore` to return **only the final `|colA - colB|`** (not cumulative). This directly targets the gap that appears before each FULL-span item. Also track column heights across segments within a single optimization call so each segment's starting heights reflect the actual column state.

### Layer 2 — Height Measurement Feedback
Capture actual rendered heights from `GridItemCard` via `onSizeChanged`. Feed these back to the optimizer so subsequent page loads use real heights instead of estimates. Since measured heights accumulate over time (as the user scrolls), each new page benefits from more accurate data.

### Layer 3 — Cross-Page Column Awareness
When loading page N, the ViewModel computes the current column heights by simulating the greedy assignment over all already-loaded items (using measured heights when available). This starting-height context is passed to the optimizer so it can make column-balancing decisions that account for the existing state.

### Layer 4 — Reduce Spacing Floor
Lower `verticalItemSpacing` from 8dp to 4dp to reduce the minimum forced gap between all items.

## Implementation Plan

### Step 1: Refactor `GridItemRepository`
- Split `dataset` into `rawDataset` (unordered) and per-page caching.
- `loadPage(page, pageSize, speed, measuredHeights, startColA, startColB)`: optimize the page's items using current context before returning.

### Step 2: Overhaul `SegmentOptimizer`
- `estimateHeight(item, measuredHeights)` — use measured height when available, fall back to formula.
- `optimize(items, measuredHeights, startColA, startColB)` — propagate column heights between segments.
- `gapScore(items, measuredHeights, startColA, startColB)` — return **final imbalance only**.
- Adjust `BASE_HEIGHT` from 80 → 88 to better match actual card layout.

### Step 3: Add Height Reporting to `GridItemCard`
- Add `onHeightMeasured: ((String, Int) -> Unit)?` parameter.
- Apply `Modifier.onSizeChanged { }` with density conversion (px → dp).

### Step 4: Track Heights in `GridViewModel`
- Add `measuredHeights: Map<String, Int>` to `GridUiState`.
- `onItemHeightMeasured(id, heightDp)` updates the map.
- `loadNextPage()` computes current column heights via greedy simulation over loaded items, then passes them to the repository.

### Step 5: Wire Up `StaggeredGridScreen`
- Pass `viewModel::onItemHeightMeasured` to each `GridItemCard`.
- Change `verticalItemSpacing` from 8dp to 4dp.

## Expected Outcome

| Gap Source | Before | After |
|---|---|---|
| Optimizer objective | Cumulative imbalance (wrong metric) | Final imbalance only (right metric) |
| Column height context | Assumes 0,0 at every call | Uses real starting heights from prior pages |
| Height accuracy | Estimated from formula | Measured actuals used for subsequent pages |
| Spacing floor | 8dp | 4dp |

Gaps won't be zero (they can't be without exact control over item heights), but they should be significantly smaller — bounded by height estimation error rather than optimizer objective mismatch.

## Files Changed

| File | Change |
|---|---|
| `SegmentOptimizer.kt` | Fix objective, add measured heights + starting column params |
| `GridItemRepository.kt` | Raw dataset + per-page optimization with context |
| `GridItemCard.kt` | Add `onHeightMeasured` callback |
| `GridViewModel.kt` | Track measured heights, compute column state, pass to repository |
| `StaggeredGridScreen.kt` | Wire height callback, reduce spacing to 4dp |
| `gradle.properties` | Added `org.gradle.java.home` pointing to JDK 21 (build toolchain fix) |

## Implementation Decisions & Deviations

### No global pre-optimization
**Original consideration:** Pre-optimize all 1000 items at startup (as the old `GridItemRepository` did with `SegmentOptimizer.optimize(DatasetGenerator.generate())`).

**Decision:** Switched to per-page optimization instead. Each page is optimized at load time using measured heights from previously rendered pages and the actual column starting heights. This is strictly better because:
- Global pre-optimization runs before any item is rendered, so it always uses estimated (inaccurate) heights
- Per-page optimization at load time uses real measured heights from all pages loaded so far
- Cross-page segment continuity is handled by passing `startColA/startColB` from the ViewModel

**Trade-off accepted:** Segments that span a page boundary (e.g. SINGLE items 18–22 where the boundary falls at item 19) are not optimized as one unit. In practice this is rare and minor: each page averages ~6 FULL-span items, so most segments fit within a page.

### No re-ordering of already-rendered items
**Considered but rejected:** Triggering a full list reorder when measured heights arrive would give better global optimality but would cause jarring visual jumps as on-screen cards animate to new positions.

**Decision:** Measured heights only influence the optimization of *future* page loads, never the order of items already in the grid.

### Hill-climbing retained, no DP partition
**Considered:** Replace hill-climbing with an exact subset-sum DP (O(n × total_height)) that finds the optimal 2-partition.

**Decision:** Kept hill-climbing with the fixed objective. The changed objective (`gapScore` = final imbalance only, not cumulative) is the dominant improvement. Hill-climbing from an LPT initial solution converges quickly for typical segment sizes (< 15 items) and avoids the complexity of constructing an ordering from a DP partition.

### `simulateColumns` made public
`SegmentOptimizer.simulateColumns` is exposed as `public` so `GridViewModel.computeColumnHeights` can reuse the same greedy simulation logic for tracking column state across the full list of loaded items, keeping the simulation consistent between optimizer and ViewModel.

### `BASE_HEIGHT` corrected 80 → 88 dp
Manually re-derived from the actual card layout: `labelSmall(16) + spacer(4) + icon(24) + spacer(4) + titleSmall(20) + spacer(4) + padding(8+8) = 88dp`. The old value caused the optimizer to underestimate card heights, biasing it toward orderings that left too much headroom.

### Build toolchain: `org.gradle.java.home` set to JDK 21
The project's Gradle 8.11.1 Kotlin DSL compiler (`JavaVersion.parse`) cannot parse the JDK 25 version string "25.0.2" — it throws `IllegalArgumentException`. JDK 21 (`/opt/homebrew/opt/openjdk@21`) was already installed. Setting `org.gradle.java.home` in `gradle.properties` routes the Gradle daemon through JDK 21 while leaving the system default JDK unchanged. The compiled app bytecode still targets JVM 11 per `compileOptions`/`kotlinOptions`.

## Build & Install Commands

```bash
# Compile only
./gradlew :app:compileDebugKotlin

# Build APK
./gradlew :app:assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.pshetye.staggeredgrid/.MainActivity
```
