package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.AlternateWinCondition
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * RULE MUTATION MOD: WIN CONDITION SHIFT
 * Temporarily changes the win condition.
 */
class WinConditionShiftMod(
    val alternateCondition: AlternateConditionType,
    val duration: Int,             // Turns the alternate condition lasts (-1 = permanent)
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "win_condition_shift"
    override val displayName = "Win Shift"
    override val icon = "🎯"
    override val category = ModCategory.RULE_MUTATION
    override val description: String
        get() {
            val condition = when (alternateCondition) {
                AlternateConditionType.THREE_IN_ROW -> "3-in-a-row wins"
                AlternateConditionType.SQUARE_PATTERN -> "2×2 square wins"
                AlternateConditionType.CORNERS -> "4 corners wins"
            }
            val dur = if (duration < 0) "permanently" else "for $duration turns"
            return "$condition $dur"
        }

    private var turnsActive = 0

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        turnsActive = 0
        
        val newCondition = when (alternateCondition) {
            AlternateConditionType.THREE_IN_ROW -> AlternateWinCondition(
                requiredInRow = 3,
                squarePatternWins = false,
                duration = duration,
                turnsRemaining = duration
            )
            AlternateConditionType.SQUARE_PATTERN -> AlternateWinCondition(
                requiredInRow = state.board.winCondition,
                squarePatternWins = true,
                duration = duration,
                turnsRemaining = duration
            )
            AlternateConditionType.CORNERS -> AlternateWinCondition(
                requiredInRow = state.board.size, // corners check is special
                squarePatternWins = false,
                duration = duration,
                turnsRemaining = duration
            )
        }
        
        return newState.setAlternateWinCondition(newCondition)
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return duration > 0 && turnsActive >= duration
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        turnsActive++
        
        // Check if alternate condition should expire
        if (duration > 0 && turnsActive >= duration) {
            val newState = state.setAlternateWinCondition(null)
            return Pair(newState, null)
        }
        
        // Update turns remaining
        val currentCondition = state.alternateWinCondition
        if (currentCondition != null && currentCondition.turnsRemaining > 0) {
            val updated = currentCondition.copy(turnsRemaining = currentCondition.turnsRemaining - 1)
            return Pair(state.setAlternateWinCondition(updated), null)
        }
        
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        val dur = if (duration < 0) "permanent" else "$duration turns"
        return "${alternateCondition.name}, $dur"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): WinConditionShiftMod {
            val conditions = if (boardSize >= 4) {
                AlternateConditionType.entries.toList()
            } else {
                listOf(AlternateConditionType.SQUARE_PATTERN) // Only square makes sense for 3x3
            }
            
            return WinConditionShiftMod(
                alternateCondition = conditions[random.nextInt(conditions.size)],
                duration = if (random.nextBoolean()) -1 else random.nextInt(3, 6),
                configuredBoardSize = boardSize
            )
        }
    }
}

enum class AlternateConditionType {
    THREE_IN_ROW,
    SQUARE_PATTERN,
    CORNERS
}
