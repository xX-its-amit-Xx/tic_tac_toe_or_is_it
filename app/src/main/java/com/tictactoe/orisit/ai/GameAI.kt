package com.tictactoe.orisit.ai

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.Player

/**
 * Simple but robust AI for Tic-Tac-Toe.
 * Strategy: Win > Block > Center > Corners > Edges
 * Adapts to board changes from mods.
 */
class GameAI(private val aiPlayer: Player = Player.O) {

    /**
     * Select the best move for the current board state.
     * Re-evaluates after any mod effects.
     */
    fun selectMove(state: GameState): Cell? {
        val board = state.board
        val emptyCells = board.getEmptyCells()

        if (emptyCells.isEmpty()) return null

        // Priority 1: Win if possible
        findWinningMove(board, aiPlayer)?.let { return it }

        // Priority 2: Block opponent's winning move
        findWinningMove(board, aiPlayer.opponent())?.let { return it }

        // Priority 3: Take center if available
        val center = Cell(1, 1)
        if (board.isEmpty(center)) return center

        // Priority 4: Take corners
        val corners = listOf(Cell(0, 0), Cell(0, 2), Cell(2, 0), Cell(2, 2))
        val availableCorners = corners.filter { board.isEmpty(it) }
        if (availableCorners.isNotEmpty()) {
            return availableCorners.random()
        }

        // Priority 5: Take any edge
        val edges = listOf(Cell(0, 1), Cell(1, 0), Cell(1, 2), Cell(2, 1))
        val availableEdges = edges.filter { board.isEmpty(it) }
        if (availableEdges.isNotEmpty()) {
            return availableEdges.random()
        }

        // Fallback: Random valid move
        return emptyCells.randomOrNull()
    }

    /**
     * Find a move that would win the game for the given player.
     */
    private fun findWinningMove(board: Board, player: Player): Cell? {
        val lines = getWinningLines()

        for (line in lines) {
            val playerCells = line.filter { board.get(it) == player && !board.isDisabled(it) }
            val emptyCells = line.filter { board.isEmpty(it) }

            if (playerCells.size == 2 && emptyCells.size == 1) {
                return emptyCells.first()
            }
        }
        return null
    }

    /**
     * Evaluate a move's strategic value (for future minimax implementation).
     */
    fun evaluateMove(board: Board, cell: Cell, player: Player): Int {
        if (!board.isEmpty(cell)) return Int.MIN_VALUE

        val testBoard = board.set(cell, player)

        // Check if this move wins
        if (testBoard.checkWinner() == player) return 100

        // Count potential winning lines through this cell
        val lines = getWinningLines().filter { cell in it }
        var score = 0

        for (line in lines) {
            val playerCount = line.count { testBoard.get(it) == player }
            val opponentCount = line.count { testBoard.get(it) == player.opponent() }
            val disabledCount = line.count { testBoard.isDisabled(it) }

            if (opponentCount == 0 && disabledCount == 0) {
                score += playerCount * 10
            }
        }

        // Bonus for center and corners
        if (cell == Cell(1, 1)) score += 15
        if (cell in listOf(Cell(0, 0), Cell(0, 2), Cell(2, 0), Cell(2, 2))) {
            score += 10
        }

        return score
    }

    /**
     * Get all winning line configurations.
     */
    private fun getWinningLines(): List<List<Cell>> {
        return listOf(
            // Rows
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),
            listOf(Cell(1, 0), Cell(1, 1), Cell(1, 2)),
            listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2)),
            // Columns
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)),
            listOf(Cell(0, 1), Cell(1, 1), Cell(2, 1)),
            listOf(Cell(0, 2), Cell(1, 2), Cell(2, 2)),
            // Diagonals
            listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2)),
            listOf(Cell(0, 2), Cell(1, 1), Cell(2, 0))
        )
    }
}
