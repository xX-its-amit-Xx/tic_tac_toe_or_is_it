package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.ModPool
import kotlin.random.Random

/**
 * Factory for creating randomized mods with their parameters.
 */
object ModFactory {

    enum class ModType {
        // Spatial Mods
        ROTATION,
        GRAVITY,
        MUTATION,
        SLIDING_ROWS,
        // Tile Behavior Mods
        FRAGILE_TILES,
        FROZEN_TILES,
        TRAP_TILES,
        // Temporal Mods
        MOVE_DECAY,
        DOUBLE_TURN,
        // Information Mods
        FOG_OF_WAR,
        DELAYED_PLACEMENT,
        // Rule Mutation Mods
        WIN_CONDITION_SHIFT,
        ASYMMETRIC_POWERS,
        // Controlled Chaos Mods
        PREDICTABLE_RANDOM,
        // Board Evolution Mods
        BOARD_RESIZE,
        TILE_DRIFT,
        // Meta Mods
        MOD_ON_MOD
    }

    private val normalMods = listOf(
        ModType.ROTATION, ModType.GRAVITY, ModType.MUTATION, ModType.SLIDING_ROWS,
        ModType.FRAGILE_TILES, ModType.FROZEN_TILES, ModType.MOVE_DECAY,
        ModType.FOG_OF_WAR, ModType.WIN_CONDITION_SHIFT
    )

    private val chaosMods = ModType.entries.toList()

    /**
     * Create a random mod with randomized parameters.
     */
    fun createRandomMod(random: Random = Random, boardSize: Int = 3, modPool: ModPool = ModPool.NORMAL): IMod {
        val availableMods = when (modPool) {
            ModPool.NORMAL -> normalMods
            ModPool.CHAOS -> chaosMods
            ModPool.CUSTOM -> normalMods
        }
        val modType = availableMods[random.nextInt(availableMods.size)]
        return createMod(modType, random, boardSize)
    }

    /**
     * Create a specific mod type with randomized parameters.
     */
    fun createMod(type: ModType, random: Random = Random, boardSize: Int = 3): IMod {
        return when (type) {
            ModType.ROTATION -> RotationMod.randomize(random, boardSize)
            ModType.GRAVITY -> GravityMod.randomize(random, boardSize)
            ModType.MUTATION -> MutationMod.randomize(random, boardSize)
            ModType.SLIDING_ROWS -> SlidingRowsMod.randomize(random, boardSize)
            ModType.FRAGILE_TILES -> FragileTilesMod.randomize(random, boardSize)
            ModType.FROZEN_TILES -> FrozenTilesMod.randomize(random, boardSize)
            ModType.TRAP_TILES -> TrapTilesMod.randomize(random, boardSize)
            ModType.MOVE_DECAY -> MoveDecayMod.randomize(random, boardSize)
            ModType.DOUBLE_TURN -> DoubleTurnMod.randomize(random, boardSize)
            ModType.FOG_OF_WAR -> FogOfWarMod.randomize(random, boardSize)
            ModType.DELAYED_PLACEMENT -> DelayedPlacementMod.randomize(random, boardSize)
            ModType.WIN_CONDITION_SHIFT -> WinConditionShiftMod.randomize(random, boardSize)
            ModType.ASYMMETRIC_POWERS -> AsymmetricPowersMod.randomize(random, boardSize)
            ModType.PREDICTABLE_RANDOM -> PredictableRandomMod.randomize(random, boardSize)
            ModType.BOARD_RESIZE -> BoardResizeMod.randomize(random, boardSize)
            ModType.TILE_DRIFT -> TileDriftMod.randomize(random, boardSize)
            ModType.MOD_ON_MOD -> ModOnModMod.randomize(random, boardSize)
        }
    }

    /**
     * Get all available mod types.
     */
    fun getAvailableModTypes(): List<ModType> = ModType.entries

    /**
     * Get mods by category.
     */
    fun getModsByCategory(category: ModCategory): List<ModType> {
        return when (category) {
            ModCategory.SPATIAL -> listOf(ModType.ROTATION, ModType.GRAVITY, ModType.MUTATION, ModType.SLIDING_ROWS)
            ModCategory.TILE_BEHAVIOR -> listOf(ModType.FRAGILE_TILES, ModType.FROZEN_TILES, ModType.TRAP_TILES)
            ModCategory.TEMPORAL -> listOf(ModType.MOVE_DECAY, ModType.DOUBLE_TURN)
            ModCategory.INFORMATION -> listOf(ModType.FOG_OF_WAR, ModType.DELAYED_PLACEMENT)
            ModCategory.RULE_MUTATION -> listOf(ModType.WIN_CONDITION_SHIFT, ModType.ASYMMETRIC_POWERS)
            ModCategory.CONTROLLED_CHAOS -> listOf(ModType.PREDICTABLE_RANDOM)
            ModCategory.BOARD_EVOLUTION -> listOf(ModType.BOARD_RESIZE, ModType.TILE_DRIFT)
            ModCategory.META -> listOf(ModType.MOD_ON_MOD)
        }
    }
}
