# LevelingStaggeredGrid Memory

## Project Overview
Android Jetpack Compose app demonstrating LazyVerticalStaggeredGrid with 2 columns, mixed SINGLE/FULL span items, and paginated loading (20 items/page from 1000-item dataset).

## Key Files
- `data/model/GridItem.kt` — `GridItem(id, title, icon, description, spanType, color)` + `SpanType(SINGLE, FULL)`
- `data/generator/DatasetGenerator.kt` — seeded 1000-item generator; ~30% FULL span, min 5 SINGLE between FULL; generates randomized pastel background color per item
- `data/generator/SegmentOptimizer.kt` — reorders SINGLE items between FULL boundaries to minimize column gaps
- `data/repository/GridItemRepository.kt` — per-page optimization with measured heights + starting column heights
- `ui/viewmodel/GridViewModel.kt` — tracks `measuredHeights`, computes column state for cross-page context
- `ui/components/GridItemCard.kt` — reports actual rendered height via `onHeightMeasured` callback
- `ui/screen/StaggeredGridScreen.kt` — main grid screen

## Architecture Patterns
- MVVM: ViewModel + StateFlow, collected with `collectAsStateWithLifecycle`
- Pagination: `shouldLoadMore` derived state triggers when within 5 items of end
- Height proxy: description text length determines card height (no explicit `heightDp` field)

## Gap Reduction Design (exec-plans/gap-reduction-plan.md)
Root cause: FULL-span items start at `max(colA, colB)`, leaving a gap in the shorter column.

Key fixes applied:
1. `gapScore` returns FINAL `|colA - colB|` only (not cumulative) — targets the actual gap metric
2. Column heights propagated between segments within `optimize()` call
3. Per-page optimization: repository optimizes page items with `measuredHeights` + `startColA/startColB`
4. `GridItemCard.onSizeChanged` feeds actual heights back to ViewModel → repository uses them for future pages
5. `verticalItemSpacing` reduced 8dp → 4dp

## Build Environment
System JDK is 25.0.2 (incompatible with Gradle 8.11.1's Kotlin DSL compiler).
**Fix:** `gradle.properties` sets `org.gradle.java.home=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home`
JDK 21 is Azul Zulu 21.46.19 at `/Library/Java/JavaVirtualMachines/zulu-21.jdk`. Build works fine with this setting.
Build commands: `./gradlew :app:assembleDebug` then `adb install -r app/build/outputs/apk/debug/app-debug.apk`
