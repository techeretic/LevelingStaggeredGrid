# LevelingStaggeredGrid

An Android application demonstrating how to minimise visual gaps in a `LazyVerticalStaggeredGrid` that mixes full-span and single-span items whose heights are unknown at load time.

## The Problem

`LazyVerticalStaggeredGrid` places SINGLE-span items greedily (shortest column first) and FULL-span items across both columns simultaneously. When a full-span item appears, it starts at the bottom of the *taller* column — leaving a gap in the shorter one:

```
Column A: [item1][item3][item5]
Column B: [item2][item4]        ← shorter
FULL item:              ↑ gap appears here
```

The gap size equals `|colA_height − colB_height|`. The only lever available is the **ordering** of SINGLE-span items within each segment (between consecutive FULL items).

## Solution: SegmentOptimizer

Since items carry no explicit height field, card height is driven entirely by **description text length** — a text-as-height proxy. The `SegmentOptimizer` uses this to find a near-optimal ordering:

1. **Segment** — Split items at each FULL-span boundary; FULL items are never reordered.
2. **LPT initial order** — Sort descending by estimated height (Longest Processing Time first, a classical 2-machine scheduling heuristic).
3. **Hill-climbing** — Try all pairwise swaps; accept any that reduce the gap score.
4. **Score = final `|colA − colB|` only** — targets the actual visible gap, not cumulative raggedness.
5. **Propagate column heights** — Each segment starts from the heights left by the previous one, not assumed-zero.

### Measured Height Feedback

As items render, `GridItemCard` reports its actual height (via `onSizeChanged`) back to the ViewModel. Subsequent page loads use these real measurements instead of formula estimates, improving optimisation accuracy progressively as the user scrolls.

### Cross-Page Column Awareness

Before loading each page, `GridViewModel` simulates the greedy column assignment over all already-loaded items to compute the current `(colA, colB)` heights. These are passed to the optimizer so it can make decisions that account for the existing grid state.

## Features

- **Gap-minimising layout** — `SegmentOptimizer` reorders items to reduce column height imbalance at each full-span boundary
- **Text-as-height proxy** — no `heightDp` field; card height is determined by description text length
- **Measured height feedback** — actual rendered heights improve optimisation for subsequent pages
- **Cross-page column awareness** — optimizer receives current column state before each page load
- **Paginated loading** — 20 items per page, auto-triggered near the scroll end
- **Simulated network delay** — toggle between Fast (200–500ms) and Slow (2–4s) to observe loading behaviour
- **Reproducible dataset** — seeded randomisation produces the same 1000 items every run
- **Item animations** — smooth fade+slide via `Modifier.animateItem()`

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM — ViewModel + StateFlow
- **Min SDK:** 26 / Target SDK: 35

## Project Structure

```
com.pshetye.staggeredgrid/
├── data/
│   ├── model/GridItem.kt                   # GridItem data class + SpanType(SINGLE, FULL)
│   ├── generator/
│   │   ├── DatasetGenerator.kt             # Seeded 1000-item generator (~30% FULL span)
│   │   └── SegmentOptimizer.kt             # Column-balancing reorder algorithm
│   └── repository/GridItemRepository.kt   # Per-page load + optimization with height context
├── ui/
│   ├── theme/                              # Material 3 colour + typography
│   ├── components/
│   │   ├── GridItemCard.kt                 # Card UI + height measurement callback
│   │   └── NetworkSpeedControls.kt         # Speed toggle + load time display
│   ├── screen/StaggeredGridScreen.kt       # Main 2-column staggered grid screen
│   └── viewmodel/GridViewModel.kt          # State, height tracking, column simulation
└── MainActivity.kt
```

## Build & Run

### Prerequisites

The Gradle build daemon requires JDK 21 (the system JDK 25.0.2 is incompatible with Gradle 8.11.1's Kotlin DSL compiler). JDK 21 is pinned via `gradle.properties` and is expected at:
```
/opt/homebrew/opt/openjdk@21
```
Install with: `brew install openjdk@21`

### CLI Build

```bash
# Compile check
./gradlew :app:compileDebugKotlin

# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.pshetye.staggeredgrid/.MainActivity
```

## Documentation

| File | Contents |
|---|---|
| `CLAUDE.md` | Project context and conventions for AI-assisted development |
| `SKILLS.md` | Techniques and patterns used, with code snippets and rationale |
| `exec-plans/gap-reduction-plan.md` | Full execution plan for the gap-reduction feature, including decisions taken |
