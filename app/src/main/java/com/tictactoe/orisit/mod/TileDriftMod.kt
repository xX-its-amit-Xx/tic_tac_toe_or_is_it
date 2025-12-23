package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.DriftDirection
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * BOARD EVOLUTION MOD: TILE DRIFT
 * Marks slowly migrate toward edges or center.
 */
class TileDriftMod(
    val driftDirection: DriftDirection,  // TO_CENTER or TO_EDGES
    val strength: Int,                    // Cells to move per trigger (1 or 2)
    val interval: Int                     // Triggers every X turns
) : BaseMod() {

    override val modId = "tile_drift"
    override val displayName = "Tile Drift"
    override val icon = "🌊"
    override val category = ModCategory.BOARD_EVOLUTION
    override val description: String
        get() {
            val dir = when (driftDirection) {
                DriftDirection.TO_CENTER -> "toward center"
                DriftDirection.TO_EDGES -> "toward edges"
            }
            return "Marks drift $dir every $interval turn(s)"
        }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % interval == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            return Pair(state, null)
        }

        val newBoard = state.board.applyDrift(driftDirection, strength)
        
        // Find affected cells for animation
        val affectedCells = mutableListOf<Int>()
        val size = state.board.size
        for (idx in 0 until size * size) {
            if (state.board.toArray()[idx] != newBoard.toArray()[idx]) {
                affectedCells.add(idx)
            }
        }
        
        val effect = ModEffect.TileDrift(driftDirection, affectedCells)
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % interval == 0) {
            return ModEffect.TileDrift(driftDirection, emptyList())
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "${driftDirection.name}, strength $strength, every $interval turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): TileDriftMod {
            return TileDriftMod(
                driftDirection = if (random.nextBoolean()) DriftDirection.TO_CENTER else DriftDirection.TO_EDGES,
                strength = random.nextInt(1, 3),
                interval = random.nextInt(2, 4)
            )
        }
    }
}
