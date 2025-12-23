package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect

/**
 * Base interface for all game mods.
 * Mods transform game rules after activation.
 */
interface IMod {
    /** Unique identifier for this mod type */
    val modId: String

    /** Display name shown to players */
    val displayName: String

    /** Emoji icon for the mod */
    val icon: String

    /** Brief description of what this mod does */
    val description: String

    /** Category of this mod for visual styling */
    val category: ModCategory

    /**
     * Called when the mod activates (turn N+1).
     * Can modify the game state immediately.
     */
    fun onActivate(state: GameState): GameState

    /**
     * Called after each turn completes (post-placement).
     * Returns the modified game state and optional effect for animation.
     */
    fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?>

    /**
     * Called before a move is made. Can intercept or modify placements.
     * Returns the potentially modified cell and whether placement should proceed.
     */
    fun onBeforeMove(state: GameState, cell: Cell): Pair<Cell, Boolean> = Pair(cell, true)

    /**
     * Called immediately after a move is made, before turn end processing.
     */
    fun onAfterMove(state: GameState, cell: Cell): GameState = state

    /**
     * Called before a turn to preview upcoming effects.
     * Used for telegraphing mutations, rotations, etc.
     */
    fun previewNextEffect(state: GameState): ModEffect?

    /**
     * Check if the mod should trigger its effect this turn.
     */
    fun shouldTrigger(state: GameState): Boolean

    /**
     * Get a human-readable summary of current parameters.
     */
    fun getParameterSummary(): String

    /**
     * Check if this mod is compatible with a given board size.
     */
    fun isCompatibleWithBoardSize(size: Int): Boolean = true
}

/**
 * Categories for visual styling and grouping of mods.
 */
enum class ModCategory(val icon: String, val colorName: String) {
    SPATIAL("🔁", "mod_spatial"),
    TILE_BEHAVIOR("🧱", "mod_tile"),
    TEMPORAL("⏱️", "mod_temporal"),
    INFORMATION("🧠", "mod_information"),
    RULE_MUTATION("⚖️", "mod_rules"),
    CONTROLLED_CHAOS("🎲", "mod_chaos"),
    BOARD_EVOLUTION("🧩", "mod_evolution"),
    META("🟣", "mod_meta")
}

/**
 * Base class providing common mod functionality.
 */
abstract class BaseMod : IMod {
    protected var activationTurn: Int = 0
    protected var boardSize: Int = 3

    override val category: ModCategory = ModCategory.SPATIAL

    override fun onActivate(state: GameState): GameState {
        activationTurn = state.turnCount
        boardSize = state.board.size
        return state.activateMod()
    }

    protected fun turnsSinceActivation(state: GameState): Int {
        return state.turnCount - activationTurn
    }

    protected fun getBoardSize(state: GameState): Int = state.board.size
}
