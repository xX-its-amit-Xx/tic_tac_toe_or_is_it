package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.TrapType
import kotlin.random.Random

/**
 * TILE BEHAVIOR MOD: TRAP TILES
 * Landing on a trap triggers an effect.
 */
class TrapTilesMod(
    val trapDensity: Int,          // Number of traps (2-4)
    val trapEffect: TrapType,
    private val seed: Long,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "trap_tiles"
    override val displayName = "Trap Tiles"
    override val icon = "💣"
    override val category = ModCategory.TILE_BEHAVIOR
    override val description: String
        get() {
            val effect = when (trapEffect) {
                TrapType.SWAP_MARKS -> "swap two random marks"
                TrapType.REMOVE_LAST_MOVE -> "remove last move"
                TrapType.FREEZE_ADJACENT -> "freeze adjacent tiles"
                TrapType.DISABLE_CELL -> "disable the cell"
            }
            return "$trapDensity hidden traps that $effect"
        }

    private val trapRandom = Random(seed)
    private var trapsPlaced = false

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        if (!trapsPlaced) {
            trapsPlaced = true
            val size = newState.board.size
            var board = newState.board
            
            val emptyCells = board.getEmptyCells()
            val trapCells = emptyCells.shuffled(trapRandom).take(trapDensity)
            
            for (cell in trapCells) {
                board = board.setTrap(cell, trapEffect)
            }
            return newState.copy(board = board)
        }
        return newState
    }

    override fun onAfterMove(state: GameState, cell: Cell): GameState {
        if (!state.modActivated) return state
        
        val trap = state.board.getTrap(cell) ?: return state
        
        var newBoard = state.board.removeTrap(cell)
        val resultDescription: String
        
        when (trap) {
            TrapType.SWAP_MARKS -> {
                val size = state.board.size
                val markedCells = (0 until size * size)
                    .filter { state.board.toArray()[it] != com.tictactoe.orisit.model.Player.NONE }
                    .shuffled(trapRandom)
                    .take(2)
                
                if (markedCells.size >= 2) {
                    newBoard = newBoard.swapMarks(markedCells[0], markedCells[1])
                    resultDescription = "Two marks swapped!"
                } else {
                    resultDescription = "Trap triggered but nothing to swap"
                }
            }
            TrapType.REMOVE_LAST_MOVE -> {
                newBoard = newBoard.removeLastMark()
                resultDescription = "Last move removed!"
            }
            TrapType.FREEZE_ADJACENT -> {
                val adjacentCells = getAdjacentCells(cell, state.board.size)
                for (adjCell in adjacentCells) {
                    if (newBoard.isEmpty(adjCell)) {
                        newBoard = newBoard.freezeCell(adjCell)
                    }
                }
                resultDescription = "Adjacent tiles frozen!"
            }
            TrapType.DISABLE_CELL -> {
                newBoard = newBoard.set(cell, com.tictactoe.orisit.model.Player.NONE).disableCell(cell)
                resultDescription = "Cell disabled!"
            }
        }
        
        val effect = ModEffect.TrapTriggered(cell.toIndex(state.board.size), trap, resultDescription)
        return state.copy(board = newBoard, lastModEffect = effect)
    }

    private fun getAdjacentCells(cell: Cell, size: Int): List<Cell> {
        val adjacent = mutableListOf<Cell>()
        val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in directions) {
            val newRow = cell.row + dr
            val newCol = cell.col + dc
            if (newRow in 0 until size && newCol in 0 until size) {
                adjacent.add(Cell(newRow, newCol))
            }
        }
        return adjacent
    }

    override fun shouldTrigger(state: GameState): Boolean = false

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        return "$trapDensity traps, ${trapEffect.name}"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): TrapTilesMod {
            val density = if (boardSize >= 4) random.nextInt(3, 6) else random.nextInt(2, 4)
            return TrapTilesMod(
                trapDensity = density,
                trapEffect = TrapType.entries[random.nextInt(TrapType.entries.size)],
                seed = random.nextLong(),
                configuredBoardSize = boardSize
            )
        }
    }
}
