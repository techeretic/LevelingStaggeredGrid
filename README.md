# LevelingStaggeredGrid

An Android application showcasing a paginated staggered grid layout built with Jetpack Compose and Material 3.

## Features

- **Staggered Grid Layout** — 2-column `LazyVerticalStaggeredGrid` with variable-height items (40–150dp)
- **Full-Span Items** — ~30% of items span both columns with a distinct `primaryContainer` background
- **Paginated Loading** — Items load 20 at a time with automatic pagination triggered near the scroll end
- **Simulated Network Delay** — Toggle between Fast (200–500ms) and Slow (2000–4000ms) to observe loading behavior
- **Load Time Tracking** — Displays wall-clock duration of the last page load
- **Item Animations** — Smooth fade+slide appearance animations via `Modifier.animateItem()`
- **Reproducible Dataset** — Seeded randomization produces the same 100 items across runs

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** ViewModel + StateFlow
- **Min SDK:** 26
- **Target/Compile SDK:** 35

## Project Structure

```
com.pshetye.staggeredgrid/
├── data/
│   ├── model/GridItem.kt              # Data class + SpanType enum
│   ├── repository/GridItemRepository.kt  # Paginated loading + NetworkSpeed enum
│   └── generator/DatasetGenerator.kt  # Seeded dataset generation (100 items)
├── ui/
│   ├── theme/                         # Material 3 dynamic color theming
│   ├── components/
│   │   ├── GridItemCard.kt            # Card rendering with icon, title, ID
│   │   └── NetworkSpeedControls.kt    # Speed toggle + load stats
│   ├── screen/StaggeredGridScreen.kt  # Main staggered grid screen
│   └── viewmodel/GridViewModel.kt     # UI state + pagination logic
└── MainActivity.kt
```

## Build & Run

1. Open the project in Android Studio
2. Sync Gradle
3. Run on an emulator or device (API 26+)
