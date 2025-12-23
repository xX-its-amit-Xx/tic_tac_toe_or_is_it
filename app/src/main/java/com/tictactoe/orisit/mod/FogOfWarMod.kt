package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * INFORMATION MOD: FOG OF WAR
 * Some tiles are hidden from view.
 */
class FogOfWarMod(
    val visibilityRadius: Int,     // Cells around last move that are visible (1-2)
    val revealTiming: RevealTiming,
    private val seed: Long,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "fog_of_war"
    override val displayName = "Fog of War"
    override val icon = "🌫️"
    override val category = ModCategory.INFORMATION
    override val description: String
        get() {
            val timing = when (revealTiming) {
                RevealTiming.ON_ADJACENT_MOVE -> "when you move nearby"
                RevealTiming.AFTER_TURN -> "after each turn"
                RevealTiming.PERMANENT -> "permanently hidden"
            }
            return "Hidden tiles reveal $timing (radius: $visibilityRadius)"
        }

    private val fogRandom = Random(seed)
    private var initialized = false

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        if (!initialized) {
            initialized = true
            val size = newState.board.size
            var board = newState.board
            
            // Hide approximately half the empty cells
            val emptyCells = board.getEmptyCells()
            val toHide = emptyCells.shuffled(fogRandom).take(emptyCells.size / 2)
            
            for (cell in toHide) {
                board = board.hideCell(cell)
            }
            return newState.copy(board = board)
        }
        return newState
    }

    override fun onAfterMove(state: GameState, cell: Cell): GameState {
        if (!state.modActivated) return state
        
        val size = state.board.size
        var newBoard = state.board
        val revealedCells = mutableSetOf<Int>()
        
        // Reveal cells within visibility radius
        for (dr in -visibilityRadius..visibilityRadius) {
            for (dc in -visibilityRadius..visibilityRadius) {
                val newRow = cell.row + dr
                val newCol = cell.col + dc
                if (newRow in 0 until size && newCol in 0 until size) {
                    val targetCell = Cell(newRow, newCol)
                    if (newBoard.isHidden(targetCell)) {
                        newBoard = newBoard.revealCell(targetCell)
                        revealedCells.add(targetCell.toIndex(size))
                    }
                }
            }
        }
        
        return state.copy(board = newBoard)
    }

    override fun shouldTrigger(state: GameState): Boolean = false

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        val hiddenCells = state.board.getHiddenCells()
        if (hiddenCells.isNotEmpty()) {
            val effect = ModEffect.FogUpdate(hiddenCells, emptySet())
            return Pair(state, effect)
        }
        
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        return "Radius: $visibilityRadius, ${revealTiming.name}"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): FogOfWarMod {
            return FogOfWarMod(
                visibilityRadius = random.nextInt(1, 3),
                revealTiming = RevealTiming.entries[random.nextInt(RevealTiming.entries.size)],
                seed = random.nextLong(),
                configuredBoardSize = boardSize
            )
        }
    }
}

enum class RevealTiming {
    ON_ADJACENT_MOVE,
    AFTER_TURN,
    PERMANENT
}
