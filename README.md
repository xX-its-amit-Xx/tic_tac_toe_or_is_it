# Tic Tac Toe... Or Is It? 🎮

A mobile-first Android Tic-Tac-Toe variant where mid-game rule modifications ("Mods") activate to twist the classic gameplay.

## Game Concept

> "I know Tic-Tac-Toe… wait, what just happened?"

Each match starts as classic 3×3 Tic-Tac-Toe, but after the first 4 turns, a hidden Mod activates with randomized parameters. Players must adapt to the changing rules to win!

## Features

- **Classic Tic-Tac-Toe base** - Familiar gameplay everyone knows
- **3 Unique Mods** - Each with randomized parameters
- **Simple AI Opponent** - Adapts to board changes
- **Smooth Animations** - Visual feedback for all mod effects
- **Quick Sessions** - Under 2 minutes per match
- **Minimalist Design** - Clean, touch-first interface

---

## The Mod System

### How It Works

1. Match starts with normal Tic-Tac-Toe rules
2. First 4 turns play normally
3. On turn 5, a random Mod activates with random parameters
4. Parameters are revealed when the Mod activates
5. Mod stays active until game end

### Mod 1: 🔄 Board Rotation

**The entire board rotates during play.**

| Parameter | Values | Description |
|-----------|--------|-------------|
| `rotation_frequency` | 1, 2, or 3 | Rotates every X turns |
| `rotation_direction` | CW / CCW | Clockwise or counter-clockwise |
| `rotation_angle` | 90° / 180° | Degrees per rotation |

**Rules:**
- All marks rotate with the board
- Input maps to post-rotation coordinates
- Rotation occurs after turn completion

### Mod 2: ⬇️ Gravity Shift

**Marks fall in a direction after placement.**

| Parameter | Values | Description |
|-----------|--------|-------------|
| `gravity_direction` | ↓ ↑ ← → | Direction marks fall |
| `gravity_strength` | 1 or 2 | Triggers every X turns |
| `lock_after_fall` | true/false | Whether marks settle permanently |

**Rules:**
- Marks slide as far as possible in the gravity direction
- Resolution order is deterministic
- Can create or break winning lines!

### Mod 3: 🧩 Board Mutation

**The board layout itself changes.**

| Parameter | Values | Description |
|-----------|--------|-------------|
| `mutation_type` | Row Swap, Column Swap, Tile Removal | How the board changes |
| `mutation_frequency` | 2 or 3 | Mutates every X turns |
| `mutation_count` | 1 or 2 | Number of elements affected |

**Rules:**
- Mutations are telegraphed 1 turn ahead (highlighted cells)
- Removed tiles become unusable
- Swapped tiles carry their marks

---

## Architecture

```
com.tictactoe.orisit/
├── model/
│   ├── Player.kt          # X, O, NONE enum
│   ├── Cell.kt            # Board position (row, col)
│   ├── Board.kt           # 3×3 grid with operations
│   ├── GameState.kt       # Current game snapshot
│   └── MatchConfig.kt     # Immutable match configuration
├── mod/
│   ├── IMod.kt            # Mod interface + BaseMod
│   ├── RotationMod.kt     # Board rotation implementation
│   ├── GravityMod.kt      # Gravity shift implementation
│   ├── MutationMod.kt     # Board mutation implementation
│   └── ModFactory.kt      # Random mod generation
├── ai/
│   └── GameAI.kt          # Simple strategic AI
├── engine/
│   └── GameEngine.kt      # Core game loop & state management
└── ui/
    ├── MainActivity.kt    # Main activity
    ├── GameViewModel.kt   # UI state management
    └── GameView.kt        # Custom board renderer
```

### Key Design Principles

1. **Immutable State** - `GameState` and `MatchConfig` are immutable
2. **Deterministic** - Same seed produces identical matches
3. **Separation of Concerns** - Input → Simulation → Rendering
4. **Extensible Mods** - Easy to add new mods via `IMod` interface

---

## Adding New Mods

### Step 1: Create the Mod Class

```kotlin
class MyNewMod(
    val myParameter: Int,
    // ... other parameters
) : BaseMod() {

    override val modId = "my_new_mod"
    override val displayName = "My New Mod"
    override val icon = "🆕"
    override val description get() = "Does something cool every $myParameter turns"

    override fun shouldTrigger(state: GameState): Boolean {
        // Return true when effect should apply
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        // Apply your transformation
        val newBoard = transformBoard(state.board)
        val effect = ModEffect.MyEffect(/* params */)
        
        return Pair(state.copy(board = newBoard), effect)
    }

    companion object {
        fun randomize(random: Random): MyNewMod {
            return MyNewMod(
                myParameter = random.nextInt(1, 4)
            )
        }
    }
}
```

### Step 2: Add to ModFactory

```kotlin
// In ModFactory.kt
enum class ModType {
    ROTATION, GRAVITY, MUTATION, MY_NEW_MOD
}

fun createRandomMod(random: Random): IMod {
    return when (ModType.entries[random.nextInt(ModType.entries.size)]) {
        // ...existing cases...
        ModType.MY_NEW_MOD -> MyNewMod.randomize(random)
    }
}
```

### Step 3: Add ModEffect (if needed)

```kotlin
// In GameState.kt
sealed class ModEffect {
    // ...existing effects...
    data class MyEffect(val params: Any) : ModEffect()
}
```

### Step 4: Handle Animation in GameView

```kotlin
// In MainActivity.kt - animateModEffect()
is ModEffect.MyEffect -> {
    binding.gameView.animateMyEffect(effect) {
        binding.gameView.setBoard(viewModel.uiState.value.gameState.board)
    }
}
```

---

## Building

### Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17+
- Android SDK 34

### Quick Start (Recommended)

1. Open the project in Android Studio
2. Let Android Studio sync Gradle (it will download the wrapper automatically)
3. Click **Run** or use `Shift+F10`

### Build Debug APK (Command Line)

```bash
# First time: Let Gradle wrapper download itself
./gradlew wrapper

# Build the APK
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK

```bash
./gradlew assembleRelease
```

### Install on Device

```bash
./gradlew installDebug
```

---

## Technical Details

| Specification | Value |
|--------------|-------|
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |
| Language | Kotlin 1.9.20 |
| Architecture | MVVM |
| UI Framework | View Binding |
| Async | Kotlin Coroutines + Flow |

---

## License

MIT License - Feel free to use, modify, and distribute.

---

## Credits

Built as a demonstration of:
- Parameterized game rule systems
- Clean Android architecture
- Deterministic game state management
- Extensible mod frameworks
