package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * TEMPORAL MOD: DOUBLE-TURN WINDOWS
 * One turn grants two placements.
 */
class DoubleTurnMod(
    val triggerTurn: Int,          // Which turn (relative to activation) triggers
    val cooldown: Int              // Turns between double-turns
) : BaseMod() {

    override val modId = "double_turn"
    override val displayName = "Double Turn"
    override val icon = "⚡"
    override val category = ModCategory.TEMPORAL
    override val description: String
        get() = "Double placement every $cooldown turns (first at turn $triggerTurn)"

    private var lastDoubleTurn = -100

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        
        // First trigger
        if (turnsSince == triggerTurn) return true
        
        // Subsequent triggers based on cooldown
        if (turnsSince > triggerTurn) {
            val turnsSinceFirst = turnsSince - triggerTurn
            return turnsSinceFirst % cooldown == 0
        }
        
        return false
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            return Pair(state, null)
        }
        
        if (state.turnCount == lastDoubleTurn) {
            return Pair(state, null)
        }
        
        lastDoubleTurn = state.turnCount
        val newState = state.enableDoubleTurn()
        val effect = ModEffect.DoubleTurn(state.currentPlayer)
        
        return Pair(newState.copy(lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurnsSince = turnsSinceActivation(state) + 1
        
        if (nextTurnsSince == triggerTurn) {
            return ModEffect.DoubleTurn(state.currentPlayer.opponent())
        }
        
        if (nextTurnsSince > triggerTurn) {
            val turnsSinceFirst = nextTurnsSince - triggerTurn
            if (turnsSinceFirst % cooldown == 0) {
                return ModEffect.DoubleTurn(state.currentPlayer.opponent())
            }
        }
        
        return null
    }

    override fun getParameterSummary(): String {
        return "First at turn $triggerTurn, then every $cooldown turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): DoubleTurnMod {
            return DoubleTurnMod(
                triggerTurn = random.nextInt(1, 4),
                cooldown = random.nextInt(3, 6)
            )
        }
    }
}
