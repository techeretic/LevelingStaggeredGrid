# CLAUDE.md — LevelingStaggeredGrid

This file gives Claude context about the project conventions, build setup, and architectural decisions so they don't need to be re-derived in every session.

## Build Environment

**System JDK:** 25.0.2 (incompatible with Gradle 8.11.1's embedded Kotlin DSL compiler)
**Fix in place:** `gradle.properties` sets `org.gradle.java.home` to the Homebrew-installed JDK 21:
```
org.gradle.java.home=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home
```
Always use `./gradlew` — never `gradle` directly. The wrapper resolves the right Gradle version.

### Key build commands
```bash
./gradlew :app:compileDebugKotlin          # fast compile check
./gradlew :app:assembleDebug               # build debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pshetye.staggeredgrid/.MainActivity
```

## Project Purpose

Demonstrates how to minimise visual gaps in a `LazyVerticalStaggeredGrid` that mixes FULL-span (2-column-wide) and SINGLE-span (1-column) items whose heights are unknown in advance — driven entirely by variable-length description text rather than an explicit `heightDp` field.

## Architecture

**Pattern:** MVVM — `GridViewModel` (StateFlow) → `StaggeredGridScreen` (Compose)
**Data flow:** `DatasetGenerator` → `GridItemRepository.loadPage()` → `StaggeredGridLeveler.optimize()` → ViewModel state → UI
**Pagination:** 20 items/page, triggered when the last visible index is within 5 of the list end.

## Key Files

| File | Role |
|---|---|
| `data/model/GridItem.kt` | `GridItem(id, title, icon, description, spanType, color)` + `SpanType(SINGLE, FULL)` |
| `data/generator/DatasetGenerator.kt` | Seeded 1000-item generator; ~30% FULL, min 5 SINGLE between each FULL; assigns randomized pastel background color per item |
| `leveling/StaggeredGridLeveler.kt` | Generic reusable optimizer — works with any item type `T` via `keySelector`, `isFullSpan`, `estimateHeight` lambdas |
| `data/repository/GridItemRepository.kt` | Loads raw pages, delegates to `StaggeredGridLeveler` for optimization |
| `ui/viewmodel/GridViewModel.kt` | Creates `StaggeredGridLeveler<GridItem>`, tracks `measuredHeights`, feeds context to repository |
| `ui/components/GridItemCard.kt` | Reports actual rendered height via `onHeightMeasured` callback; uses `item.color` for card background |
| `ui/screen/StaggeredGridScreen.kt` | 2-column `LazyVerticalStaggeredGrid`, wires height callbacks, 4dp vertical spacing |

## Gap-Reduction Algorithm (StaggeredGridLeveler)

`StaggeredGridLeveler<T>` is a **generic API** that works with any item type. It takes three lambdas:
- `keySelector: (T) -> String` — unique ID for measured-height lookup
- `isFullSpan: (T) -> Boolean` — identifies full-span items (segment boundaries)
- `estimateHeight: (T) -> Int` — content-based height estimate when no measurement exists

Items between each pair of full-span boundaries form a *segment*. Within each segment:

1. **Resolves height** via `resolveHeight()`: uses measured actual height if available, otherwise the `estimateHeight` lambda. For this app: `BASE_HEIGHT(88) + LINE_HEIGHT(16) × ceil(chars / 23)`.
2. **Initial order:** Sort descending by height (LPT heuristic — strong for 2-machine scheduling).
3. **Hill-climbing:** Try all pairwise swaps; keep any that reduce the score.
4. **Score = final `|colA − colB|` only** — the gap that actually appears before the next FULL item.
5. **Starting heights propagated:** Each segment starts with the column heights left by the previous segment, not assumed-zero. Full-span items level both columns (`max(colA, colB) + fullH`).

### Cross-page awareness
`StaggeredGridLeveler.computeColumnHeights()` simulates the greedy assignment over all loaded items (handling both single-span and full-span items), then the ViewModel passes `(startColA, startColB)` to `GridItemRepository.loadPage()` for each new page.

## Conventions

- No explicit `heightDp` on data models — text content length is the sole height driver.
- `measuredHeights: Map<String, Int>` flows: `GridItemCard.onSizeChanged` → `GridViewModel.onItemHeightMeasured` → `GridItemRepository.loadPage` → `StaggeredGridLeveler.optimize`.
- Already-rendered items are **never** reordered (would cause jarring visual jumps).
- `verticalItemSpacing = 4.dp` (reduced from 8dp to lower the minimum forced gap).

## Execution Plans

Stored in `exec-plans/`. Each plan documents the problem, chosen approach, implementation steps, decisions taken, and deviations from the original plan.

- `exec-plans/gap-reduction-plan.md` — gap minimisation feature

## Memory

`memory/MEMORY.md` — auto-maintained by Claude across sessions; contains concise notes on project structure, patterns, and the build environment.
