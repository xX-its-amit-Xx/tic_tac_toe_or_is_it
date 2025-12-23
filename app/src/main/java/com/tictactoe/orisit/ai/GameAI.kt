package com.tictactoe.orisit.ai

import com.tictactoe.orisit.model.AIDifficulty
import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * Heuristic-based AI for Tic-Tac-Toe with mod awareness.
 * Adapts to configurable board sizes and mod effects.
 * Strategy: Win > Block > Strategic positions > Mod-aware survival
 */
class GameAI(
    private val aiPlayer: Player = Player.O,
    private val difficulty: AIDifficulty = AIDifficulty.NORMAL
) {
    private val random = Random(System.currentTimeMillis())

    /**
     * Select the best move for the current board state.
     * Re-evaluates after any mod effects.
     */
    fun selectMove(state: GameState): Cell? {
        val board = state.board
        val emptyCells = board.getEmptyCells()

        if (emptyCells.isEmpty()) return null

        // Easy mode: Sometimes make suboptimal moves
        if (difficulty == AIDifficulty.EASY && random.nextFloat() < 0.3f) {
            return emptyCells.random()
        }

        val winCondition = state.getEffectiveWinCondition()

        // Priority 1: Win if possible
        findWinningMove(board, aiPlayer, winCondition)?.let { return it }

        // Priority 2: Block opponent's winning move
        findWinningMove(board, aiPlayer.opponent(), winCondition)?.let { return it }

        // Priority 3: Check for square win if applicable
        if (state.shouldCheckSquareWin()) {
            findSquareWinMove(board, aiPlayer)?.let { return it }
            findSquareWinMove(board, aiPlayer.opponent())?.let { return it }
        }

        // Priority 4: Strategic positioning based on board state
        val strategicMove = findStrategicMove(state, emptyCells)
        if (strategicMove != null) return strategicMove

        // Priority 5: Mod-aware survival
        val safestMove = findSafestMove(state, emptyCells)
        if (safestMove != null) return safestMove

        // Fallback: Best scored empty cell
        return emptyCells.maxByOrNull { evaluateMove(board, it, aiPlayer, winCondition) }
            ?: emptyCells.randomOrNull()
    }

    /**
     * Find a move that would win the game for the given player.
     */
    private fun findWinningMove(board: Board, player: Player, winCondition: Int): Cell? {
        val lines = board.generateWinningLines(winCondition)

        for (line in lines) {
            val playerCells = line.filter { board.get(it) == player && !board.isDisabled(it) }
            val emptyCells = line.filter { board.isEmpty(it) }

            if (playerCells.size == winCondition - 1 && emptyCells.size == 1) {
                return emptyCells.first()
            }
        }
        return null
    }

    /**
     * Find a move that would complete a 2x2 square.
     */
    private fun findSquareWinMove(board: Board, player: Player): Cell? {
        val size = board.size
        for (row in 0 until size - 1) {
            for (col in 0 until size - 1) {
                val square = listOf(
                    Cell(row, col), Cell(row, col + 1),
                    Cell(row + 1, col), Cell(row + 1, col + 1)
                )
                val playerCells = square.filter { board.get(it) == player }
                val emptyCells = square.filter { board.isEmpty(it) }
                
                if (playerCells.size == 3 && emptyCells.size == 1) {
                    return emptyCells.first()
                }
            }
        }
        return null
    }

    /**
     * Find strategic positions based on board state.
     */
    private fun findStrategicMove(state: GameState, emptyCells: List<Cell>): Cell? {
        val board = state.board
        val size = board.size

        // Center control - adapt to board size
        val centers = getCenterCells(size)
        val availableCenters = centers.filter { board.isEmpty(it) }
        if (availableCenters.isNotEmpty()) {
            return availableCenters.random()
        }

        // Corner control
        val corners = getCornerCells(size)
        val availableCorners = corners.filter { board.isEmpty(it) }
        if (availableCorners.isNotEmpty()) {
            // Prefer corners that create more winning opportunities
            return availableCorners.maxByOrNull { 
                evaluateMove(board, it, aiPlayer, state.getEffectiveWinCondition()) 
            }
        }

        // Edge positions
        val edges = getEdgeCells(size)
        val availableEdges = edges.filter { board.isEmpty(it) }
        if (availableEdges.isNotEmpty()) {
            return availableEdges.random()
        }

        return null
    }

    /**
     * Find the safest move considering mod effects.
     */
    private fun findSafestMove(state: GameState, emptyCells: List<Cell>): Cell? {
        val board = state.board
        
        // Avoid frozen cells
        val nonFrozenCells = emptyCells.filter { !board.isFrozen(it) }
        if (nonFrozenCells.isEmpty()) return null

        // Avoid trap cells if we can detect them (we can't see hidden traps)
        val safeCells = nonFrozenCells.filter { board.getTrap(it) == null }
        
        // Avoid cells that might be affected by pending mutations
        val pendingMutation = state.pendingMutation
        val saferCells = if (pendingMutation != null) {
            safeCells.filter { cell ->
                cell.toIndex(board.size) !in pendingMutation.affectedIndices
            }
        } else {
            safeCells
        }

        // Prefer cells with higher durability or no durability tracking
        return saferCells.maxByOrNull { cell ->
            val durability = board.getDurability(cell)
            if (durability < 0) 100 else durability
        } ?: safeCells.randomOrNull() ?: nonFrozenCells.randomOrNull()
    }

    /**
     * Evaluate a move's strategic value.
     */
    fun evaluateMove(board: Board, cell: Cell, player: Player, winCondition: Int): Int {
        if (!board.isEmpty(cell)) return Int.MIN_VALUE

        val testBoard = board.set(cell, player)
        val size = board.size

        // Check if this move wins
        if (testBoard.checkWinner(winCondition) == player) return 1000

        // Count potential winning lines through this cell
        val lines = board.generateWinningLines(winCondition).filter { cell in it }
        var score = 0

        for (line in lines) {
            val playerCount = line.count { testBoard.get(it) == player }
            val opponentCount = line.count { testBoard.get(it) == player.opponent() }
            val disabledCount = line.count { testBoard.isDisabled(it) }
            val frozenCount = line.count { testBoard.isFrozen(it) }

            if (opponentCount == 0 && disabledCount == 0) {
                score += playerCount * 10
                // Bonus for lines with more of our marks
                if (playerCount >= winCondition - 2) score += 20
            }
            
            // Penalty for frozen cells in line
            score -= frozenCount * 5
        }

        // Bonus for center positions
        val centers = getCenterCells(size)
        if (cell in centers) score += 15

        // Bonus for corner positions
        val corners = getCornerCells(size)
        if (cell in corners) score += 10

        // Penalty for cells with low durability
        val durability = board.getDurability(cell)
        if (durability in 1..2) score -= 15

        // Penalty for trapped cells
        if (board.getTrap(cell) != null) score -= 30

        return score
    }

    /**
     * Get center cells for the given board size.
     */
    private fun getCenterCells(size: Int): List<Cell> {
        return when (size) {
            3 -> listOf(Cell(1, 1))
            4 -> listOf(Cell(1, 1), Cell(1, 2), Cell(2, 1), Cell(2, 2))
            5 -> listOf(Cell(2, 2), Cell(1, 2), Cell(2, 1), Cell(2, 3), Cell(3, 2))
            else -> {
                val mid = size / 2
                listOf(Cell(mid, mid))
            }
        }
    }

    /**
     * Get corner cells for the given board size.
     */
    private fun getCornerCells(size: Int): List<Cell> {
        val last = size - 1
        return listOf(
            Cell(0, 0), Cell(0, last),
            Cell(last, 0), Cell(last, last)
        )
    }

    /**
     * Get edge cells (non-corner border cells) for the given board size.
     */
    private fun getEdgeCells(size: Int): List<Cell> {
        val edges = mutableListOf<Cell>()
        val last = size - 1
        
        // Top and bottom edges (excluding corners)
        for (col in 1 until last) {
            edges.add(Cell(0, col))
            edges.add(Cell(last, col))
        }
        
        // Left and right edges (excluding corners)
        for (row in 1 until last) {
            edges.add(Cell(row, 0))
            edges.add(Cell(row, last))
        }
        
        return edges
    }

    /**
     * Create a threat by placing a mark that creates multiple winning opportunities.
     */
    fun findForkMove(board: Board, player: Player, winCondition: Int): Cell? {
        val emptyCells = board.getEmptyCells()
        
        for (cell in emptyCells) {
            val testBoard = board.set(cell, player)
            var winningOpportunities = 0
            
            val lines = testBoard.generateWinningLines(winCondition)
            for (line in lines) {
                val playerCells = line.filter { testBoard.get(it) == player }
                val emptyCellsInLine = line.filter { testBoard.isEmpty(it) }
                
                if (playerCells.size == winCondition - 1 && emptyCellsInLine.size == 1) {
                    winningOpportunities++
                }
            }
            
            if (winningOpportunities >= 2) {
                return cell
            }
        }
        
        return null
    }
}
