# Tic Tac Toe... Or Is It? 🎮

A mobile-first Android Tic-Tac-Toe variant where mid-game rule modifications ("Mods") activate to twist the classic gameplay. Now with **4×4 boards**, **15+ unique Mods**, and **Bluetooth multiplayer**!

## Game Concept

> "Every match teaches me a new way to think."

Each match starts with familiar Tic-Tac-Toe rules, but after the first few turns, a hidden Mod activates with randomized parameters. Players must adapt to the changing rules to win!

## Features

- **Configurable Board Sizes** - 3×3 classic or 4×4 extended
- **15+ Unique Mods** - Organized into 8 strategic categories
- **Multiple Game Modes** - Play vs AI, Local, or Bluetooth multiplayer
- **Heuristic AI** - Adapts to board size, mod effects, and strategic threats
- **Smooth Animations** - Visual feedback for all mod effects
- **Quick Sessions** - Under 3 minutes per match
- **Minimalist Design** - Clean, touch-first interface

---

## Game Modes

| Mode | Description |
|------|-------------|
| **Play vs AI** | Single player against adaptive AI |
| **Local Multiplayer** | Pass-and-play on one device |
| **Bluetooth Multiplayer** | Peer-to-peer local wireless play |

### Board Sizes

- **3×3 Classic** - Win with 3 in a row
- **4×4 Extended** - Win with 4 in a row (default)

### Mod Pools

- **Normal** - Balanced selection of mods
- **Chaos** - All mods including experimental ones

---

## The Mod System

### How It Works

1. Match starts with normal Tic-Tac-Toe rules
2. First N turns play normally (N=4 for 3×3, N=5 for 4×4)
3. A random Mod activates with random parameters
4. Parameters are revealed when the Mod activates
5. Mod stays active until game end

---

## Mod Categories

### 🔁 SPATIAL MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Dynamic Rotation** | Board or regions rotate | `frequency`, `angle`, `region`, `direction` |
| **Gravity Shift** | Marks fall in a direction | `direction`, `strength`, `lock_after_fall` |
| **Sliding Rows** | Rows/columns slide cyclically | `row_count`, `direction`, `interval` |
| **Board Mutation** | Board layout changes | `mutation_type`, `frequency`, `count` |

### 🧱 TILE BEHAVIOR MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Fragile Tiles** | Tiles break after X uses | `durability`, `break_behavior` |
| **Frozen Tiles** | Tiles temporarily unplayable | `freeze_duration`, `freeze_pattern` |
| **Trap Tiles** | Hidden effects on placement | `trap_density`, `trap_effect` |

### ⏱️ TEMPORAL MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Move Decay** | Marks fade after X turns | `decay_time`, `decay_order` |
| **Double Turn** | One turn grants two placements | `trigger_turn`, `cooldown` |

### 🧠 INFORMATION MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Fog of War** | Some tiles hidden | `visibility_radius`, `reveal_timing` |
| **Delayed Placement** | Marks appear after delay | `delay_turns` |

### ⚖️ RULE MUTATION MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Win Condition Shift** | Alternate win conditions | `alternate_condition`, `duration` |
| **Asymmetric Powers** | Each player gets abilities | `ability_type`, `uses` |

### 🎲 CONTROLLED CHAOS MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Predictable Random** | Random effects previewed ahead | `effect_pool`, `preview_duration` |

### 🧩 BOARD EVOLUTION MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Board Growth/Shrink** | Board size changes | `new_size`, `duration` |
| **Tile Drift** | Marks migrate toward edges/center | `drift_direction`, `strength` |

### 🟣 META MODS

| Mod | Description | Parameters |
|-----|-------------|------------|
| **Mod-on-Mod** | Secondary micro-mod activates | `secondary_mod_type`, `duration` |

---

## Architecture

```
com.tictactoe.orisit/
├── model/
│   ├── Player.kt          # X, O, NONE enum
│   ├── Cell.kt            # Board position (row, col) - size-aware
│   ├── Board.kt           # NxN grid with operations
│   ├── GameState.kt       # Current game snapshot + mod states
│   └── MatchConfig.kt     # Immutable match configuration
├── mod/
│   ├── IMod.kt            # Mod interface + BaseMod + ModCategory
│   ├── ModFactory.kt      # Random mod generation with pools
│   ├── RotationMod.kt     # Dynamic rotation (full/partial)
│   ├── GravityMod.kt      # Gravity shift
│   ├── MutationMod.kt     # Board mutation
│   ├── SlidingRowsMod.kt  # Sliding rows/columns
│   ├── FragileTilesMod.kt # Fragile tiles
│   ├── FrozenTilesMod.kt  # Frozen tiles
│   ├── TrapTilesMod.kt    # Trap tiles
│   ├── MoveDecayMod.kt    # Move decay
│   ├── DoubleTurnMod.kt   # Double turn windows
│   ├── FogOfWarMod.kt     # Fog of war
│   ├── DelayedPlacementMod.kt  # Delayed placement
│   ├── WinConditionShiftMod.kt # Win condition shift
│   ├── AsymmetricPowersMod.kt  # Asymmetric powers
│   ├── PredictableRandomMod.kt # Predictable random
│   ├── BoardResizeMod.kt  # Board growth/shrink
│   ├── TileDriftMod.kt    # Tile drift
│   └── ModOnModMod.kt     # Mod-on-mod
├── ai/
│   └── GameAI.kt          # Heuristic AI with mod awareness
├── engine/
│   └── GameEngine.kt      # Core game loop & state management
├── bluetooth/
│   ├── BluetoothManager.kt     # Bluetooth connection handling
│   └── MatchSyncController.kt  # Turn-based synchronization
└── ui/
    ├── MainMenuActivity.kt     # Main menu & mode selection
    ├── BluetoothSetupActivity.kt # Bluetooth pairing UI
    ├── HowToPlayActivity.kt    # Game instructions
    ├── MainActivity.kt         # Game activity
    ├── GameViewModel.kt        # UI state management
    └── GameView.kt             # Custom board renderer (NxN)
```

### Key Design Principles

1. **Immutable State** - `GameState` and `MatchConfig` are immutable
2. **Deterministic** - Same seed produces identical matches
3. **Separation of Concerns** - Input → Simulation → Rendering
4. **Extensible Mods** - Easy to add new mods via `IMod` interface
5. **Board Size Agnostic** - All logic works with configurable board sizes

---

## Bluetooth Architecture

### Overview

Bluetooth multiplayer uses peer-to-peer RFCOMM connections with turn-based lockstep synchronization.

```
┌─────────────────┐         ┌─────────────────┐
│     HOST        │◄───────►│     GUEST       │
│  (Player X)     │ RFCOMM  │  (Player O)     │
└─────────────────┘         └─────────────────┘
        │                           │
        ▼                           ▼
┌─────────────────┐         ┌─────────────────┐
│ BluetoothManager│         │ BluetoothManager│
│ MatchSyncController       │ MatchSyncController
└─────────────────┘         └─────────────────┘
```

### Connection Flow

1. **Host** starts listening with `BluetoothManager.startHosting()`
2. **Guest** connects via `BluetoothManager.connectToDevice()`
3. Host sends `MatchConfig` (seed, board size, mod params)
4. Both create identical game state from shared seed
5. Moves are synchronized via `BluetoothMessage.Move`

### Message Protocol

| Message | Format | Purpose |
|---------|--------|---------|
| CONFIG | `CONFIG:seed:size:winCond:activationTurn` | Share match config |
| MOVE | `MOVE:row:col:turnNumber` | Sync player moves |
| END | `END:winner` | Game end notification |
| READY | `READY` | Handshake signal |
| PING | `PING` | Keep-alive |

---

## Adding New Mods

### Step 1: Create the Mod Class

```kotlin
class MyNewMod(
    val myParameter: Int,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "my_new_mod"
    override val displayName = "My New Mod"
    override val icon = "🆕"
    override val category = ModCategory.SPATIAL  // Choose appropriate category
    override val description get() = "Does something cool every $myParameter turns"

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % myParameter == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        // Apply transformation - use getBoardSize(state) for board operations
        val newBoard = transformBoard(state.board)
        val effect = ModEffect.MyEffect(/* params */)
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        // Return preview for next turn if applicable
        return null
    }

    override fun getParameterSummary(): String {
        return "Every $myParameter turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): MyNewMod {
            return MyNewMod(
                myParameter = random.nextInt(1, 4),
                configuredBoardSize = boardSize
            )
        }
    }
}
```

### Step 2: Add to ModFactory

```kotlin
// In ModFactory.kt
enum class ModType {
    // ... existing types ...
    MY_NEW_MOD
}

// In createMod() function:
ModType.MY_NEW_MOD -> MyNewMod.randomize(random, boardSize)

// Add to appropriate mod pool list if needed
```

### Step 3: Add ModEffect (if needed)

```kotlin
// In GameState.kt
sealed class ModEffect {
    // ... existing effects ...
    data class MyEffect(val params: Any) : ModEffect()
}
```

### Step 4: Handle Animation in UI (optional)

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

### Quick Start

1. Open the project in Android Studio
2. Let Android Studio sync Gradle
3. Click **Run** or use `Shift+F10`

### Build APK

```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

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
| Bluetooth | RFCOMM (SPP) |

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
- Bluetooth peer-to-peer multiplayer
