package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.SubModEffect
import kotlin.random.Random

/**
 * META MOD: MOD-ON-MOD
 * Secondary micro-mod activates briefly.
 */
class ModOnModMod(
    val secondaryModType: SecondaryModType,
    val duration: Int,             // Turns the secondary mod is active
    val triggerInterval: Int,      // Turns between secondary mod activations
    private val seed: Long
) : BaseMod() {

    override val modId = "mod_on_mod"
    override val displayName = "Mod Fusion"
    override val icon = "🟣"
    override val category = ModCategory.META
    override val description: String
        get() = "${secondaryModType.displayName} activates for $duration turns every $triggerInterval turns"

    private val modRandom = Random(seed)
    private var secondaryActive = false
    private var secondaryTurnsRemaining = 0

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return !secondaryActive && turnsSince > 0 && turnsSince % triggerInterval == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        var newState = state
        var effect: ModEffect? = null
        
        // Handle secondary mod expiration
        if (secondaryActive) {
            secondaryTurnsRemaining--
            if (secondaryTurnsRemaining <= 0) {
                secondaryActive = false
                newState = newState.copy(activeSubMod = null)
            } else {
                // Apply secondary mod effect
                val (stateAfterSecondary, secondaryEffect) = applySecondaryEffect(newState)
                newState = stateAfterSecondary
                effect = secondaryEffect
            }
        }
        
        // Check for new secondary mod activation
        if (shouldTrigger(state)) {
            secondaryActive = true
            secondaryTurnsRemaining = duration
            
            newState = newState.copy(
                activeSubMod = SubModEffect(
                    modName = secondaryModType.displayName,
                    turnsRemaining = duration
                )
            )
            
            effect = ModEffect.SubModActivated(secondaryModType.displayName, duration)
        }
        
        // Update turns remaining in state
        if (secondaryActive && newState.activeSubMod != null) {
            newState = newState.copy(
                activeSubMod = newState.activeSubMod.copy(turnsRemaining = secondaryTurnsRemaining)
            )
        }
        
        return Pair(newState, effect)
    }

    private fun applySecondaryEffect(state: GameState): Pair<GameState, ModEffect?> {
        return when (secondaryModType) {
            SecondaryModType.MINI_GRAVITY -> {
                val directions = com.tictactoe.orisit.model.GravityDirection.entries
                val dir = directions[modRandom.nextInt(directions.size)]
                val newBoard = state.board.applyGravity(dir)
                Pair(state.copy(board = newBoard), ModEffect.Gravity(dir))
            }
            SecondaryModType.MINI_ROTATION -> {
                val newBoard = if (modRandom.nextBoolean()) {
                    state.board.rotate90Clockwise()
                } else {
                    state.board.rotate90CounterClockwise()
                }
                Pair(state.copy(board = newBoard), ModEffect.Rotation(90, true))
            }
            SecondaryModType.MINI_FREEZE -> {
                val emptyCells = state.board.getEmptyCells()
                if (emptyCells.isNotEmpty()) {
                    val cellToFreeze = emptyCells[modRandom.nextInt(emptyCells.size)]
                    val newBoard = state.board.freezeCell(cellToFreeze)
                    Pair(
                        state.copy(board = newBoard),
                        ModEffect.TileFreeze(listOf(cellToFreeze.toIndex(state.board.size)), 2)
                    )
                } else {
                    Pair(state, null)
                }
            }
            SecondaryModType.MINI_DRIFT -> {
                val dir = if (modRandom.nextBoolean()) {
                    com.tictactoe.orisit.model.DriftDirection.TO_CENTER
                } else {
                    com.tictactoe.orisit.model.DriftDirection.TO_EDGES
                }
                val newBoard = state.board.applyDrift(dir, 1)
                Pair(state.copy(board = newBoard), ModEffect.TileDrift(dir, emptyList()))
            }
        }
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        
        if (secondaryActive && secondaryTurnsRemaining > 0) {
            return ModEffect.SubModActivated(secondaryModType.displayName, secondaryTurnsRemaining)
        }
        
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % triggerInterval == 0) {
            return ModEffect.SubModActivated(secondaryModType.displayName, duration)
        }
        
        return null
    }

    override fun getParameterSummary(): String {
        return "${secondaryModType.displayName}, $duration turns, every $triggerInterval"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): ModOnModMod {
            return ModOnModMod(
                secondaryModType = SecondaryModType.entries[random.nextInt(SecondaryModType.entries.size)],
                duration = random.nextInt(2, 4),
                triggerInterval = random.nextInt(3, 6),
                seed = random.nextLong()
            )
        }
    }
}

enum class SecondaryModType(val displayName: String) {
    MINI_GRAVITY("Mini Gravity"),
    MINI_ROTATION("Mini Rotation"),
    MINI_FREEZE("Mini Freeze"),
    MINI_DRIFT("Mini Drift")
}
