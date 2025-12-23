package com.tictactoe.orisit.mod

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
}

/**
 * Base class providing common mod functionality.
 */
abstract class BaseMod : IMod {
    protected var activationTurn: Int = 0

    override fun onActivate(state: GameState): GameState {
        activationTurn = state.turnCount
        return state.activateMod()
    }

    protected fun turnsSinceActivation(state: GameState): Int {
        return state.turnCount - activationTurn
    }
}
