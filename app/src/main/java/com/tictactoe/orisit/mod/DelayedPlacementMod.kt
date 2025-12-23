package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.DelayedPlacement
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * INFORMATION MOD: DELAYED PLACEMENT
 * Marks appear one turn after placement.
 */
class DelayedPlacementMod(
    val delayTurns: Int           // Turns before mark appears (1 or 2)
) : BaseMod() {

    override val modId = "delayed_placement"
    override val displayName = "Delayed Placement"
    override val icon = "⏰"
    override val category = ModCategory.INFORMATION
    override val description: String
        get() = "Marks appear after $delayTurns turn(s)"

    override fun onBeforeMove(state: GameState, cell: Cell): Pair<Cell, Boolean> {
        if (!state.modActivated) return Pair(cell, true)
        
        // Mark the cell as having a delayed placement instead of immediate
        return Pair(cell, false) // Don't place immediately
    }

    override fun onAfterMove(state: GameState, cell: Cell): GameState {
        if (!state.modActivated) return state
        
        // Add delayed placement
        val delayed = DelayedPlacement(
            cell = cell,
            player = state.currentPlayer,
            placedTurn = state.turnCount,
            revealTurn = state.turnCount + delayTurns
        )
        
        val newBoard = state.board.addDelayedPlacement(delayed)
        return state.copy(board = newBoard)
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return state.board.getDelayedPlacements().any { it.revealTurn <= state.turnCount }
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        val newBoard = state.board.processDelayedPlacements(state.turnCount)
        val revealed = state.board.getDelayedPlacements()
            .filter { it.revealTurn <= state.turnCount }
        
        if (revealed.isNotEmpty()) {
            val first = revealed.first()
            val effect = ModEffect.DelayedReveal(first.cell, first.player)
            return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
        }
        
        return Pair(state.copy(board = newBoard), null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        
        val willReveal = state.board.getDelayedPlacements()
            .filter { it.revealTurn == state.turnCount + 1 }
        
        if (willReveal.isNotEmpty()) {
            val first = willReveal.first()
            return ModEffect.DelayedReveal(first.cell, first.player)
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "Delay: $delayTurns turn(s)"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): DelayedPlacementMod {
            return DelayedPlacementMod(
                delayTurns = random.nextInt(1, 3)
            )
        }
    }
}
