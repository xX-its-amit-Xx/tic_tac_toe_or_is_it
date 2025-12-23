package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * TEMPORAL MOD: MOVE DECAY
 * Marks fade after X turns.
 */
class MoveDecayMod(
    val decayTime: Int,            // Turns before decay (3-5)
    val decayOrder: DecayOrder     // OLDEST_FIRST or NEWEST_FIRST
) : BaseMod() {

    override val modId = "move_decay"
    override val displayName = "Move Decay"
    override val icon = "⏳"
    override val category = ModCategory.TEMPORAL
    override val description: String
        get() {
            val order = when (decayOrder) {
                DecayOrder.OLDEST_FIRST -> "oldest"
                DecayOrder.NEWEST_FIRST -> "newest"
            }
            return "Marks fade after $decayTime turns ($order first)"
        }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return true // Check every turn
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        val size = state.board.size
        val currentTurn = state.turnCount
        val decayedCells = mutableListOf<Int>()
        
        var newBoard = state.board
        val cellsWithTurns = mutableListOf<Pair<Int, Int>>() // (index, placementTurn)
        
        for (idx in 0 until size * size) {
            val cell = Cell.fromIndex(idx, size)
            val placementTurn = state.board.getPlacementTurn(cell)
            if (placementTurn >= 0 && state.board.get(cell) != Player.NONE) {
                cellsWithTurns.add(Pair(idx, placementTurn))
            }
        }
        
        // Sort based on decay order
        val sortedCells = when (decayOrder) {
            DecayOrder.OLDEST_FIRST -> cellsWithTurns.sortedBy { it.second }
            DecayOrder.NEWEST_FIRST -> cellsWithTurns.sortedByDescending { it.second }
        }
        
        // Decay marks that are too old
        for ((idx, placementTurn) in sortedCells) {
            val age = currentTurn - placementTurn
            if (age >= decayTime) {
                val cell = Cell.fromIndex(idx, size)
                newBoard = newBoard.set(cell, Player.NONE)
                decayedCells.add(idx)
            }
        }
        
        if (decayedCells.isNotEmpty()) {
            val effect = ModEffect.MoveDecay(decayedCells)
            return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
        }
        
        return Pair(state.copy(board = newBoard), null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        
        // Preview cells that will decay next turn
        val size = state.board.size
        val nextTurn = state.turnCount + 1
        val willDecay = mutableListOf<Int>()
        
        for (idx in 0 until size * size) {
            val cell = Cell.fromIndex(idx, size)
            val placementTurn = state.board.getPlacementTurn(cell)
            if (placementTurn >= 0) {
                val ageNextTurn = nextTurn - placementTurn
                if (ageNextTurn >= decayTime && state.board.get(cell) != Player.NONE) {
                    willDecay.add(idx)
                }
            }
        }
        
        if (willDecay.isNotEmpty()) {
            return ModEffect.MoveDecay(willDecay)
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "Decay after $decayTime turns, ${decayOrder.name}"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): MoveDecayMod {
            val decayTime = if (boardSize >= 4) random.nextInt(4, 7) else random.nextInt(3, 5)
            return MoveDecayMod(
                decayTime = decayTime,
                decayOrder = if (random.nextBoolean()) DecayOrder.OLDEST_FIRST else DecayOrder.NEWEST_FIRST
            )
        }
    }
}

enum class DecayOrder {
    OLDEST_FIRST,
    NEWEST_FIRST
}
