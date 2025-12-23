package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * Chain Reaction Mod - When 3+ marks of same type are adjacent, one spreads to an empty neighbor.
 */
class ChainReactionMod(
    val threshold: Int = 3,
    private val configuredBoardSize: Int = 3,
    private val seed: Long = System.currentTimeMillis()
) : BaseMod() {

    override val modId = "chain_reaction"
    override val displayName = "Chain Reaction"
    override val icon = "⚡"
    override val category = ModCategory.RULE_MUTATION
    override val description get() = "When $threshold+ adjacent marks exist, one spreads to empty neighbor"

    private var internalRandom = Random(seed)

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return true // Check every turn
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        val newBoard = checkAndSpread(state.board)
        if (newBoard == state.board) return Pair(state, null)
        
        val effect = ModEffect.RandomEffect(
            effectName = "Chain Reaction",
            description = "Marks spread!"
        )
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    private fun checkAndSpread(board: Board): Board {
        val size = board.size
        
        for (player in listOf(Player.X, Player.O)) {
            // Find clusters of marks
            for (row in 0 until size) {
                for (col in 0 until size) {
                    if (board.get(row, col) == player) {
                        val cluster = findCluster(board, row, col, player)
                        if (cluster.size >= threshold) {
                            // Find empty adjacent cells to the cluster
                            val emptyAdjacents = mutableSetOf<Cell>()
                            for (cell in cluster) {
                                val adjacents = listOf(
                                    Cell(cell.row - 1, cell.col),
                                    Cell(cell.row + 1, cell.col),
                                    Cell(cell.row, cell.col - 1),
                                    Cell(cell.row, cell.col + 1)
                                ).filter { it.isValid(size) && 
                                          board.get(it.row, it.col) == Player.NONE &&
                                          !board.isDisabled(it) }
                                emptyAdjacents.addAll(adjacents)
                            }
                            
                            if (emptyAdjacents.isNotEmpty()) {
                                val target = emptyAdjacents.toList()[internalRandom.nextInt(emptyAdjacents.size)]
                                return board.set(target, player)
                            }
                        }
                    }
                }
            }
        }
        
        return board
    }

    private fun findCluster(board: Board, startRow: Int, startCol: Int, player: Player): Set<Cell> {
        val size = board.size
        val cluster = mutableSetOf<Cell>()
        val toVisit = mutableListOf(Cell(startRow, startCol))
        
        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeAt(0)
            if (current in cluster) continue
            if (!current.isValid(size)) continue
            if (board.get(current.row, current.col) != player) continue
            
            cluster.add(current)
            
            toVisit.add(Cell(current.row - 1, current.col))
            toVisit.add(Cell(current.row + 1, current.col))
            toVisit.add(Cell(current.row, current.col - 1))
            toVisit.add(Cell(current.row, current.col + 1))
        }
        
        return cluster
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        return "Spread at $threshold+ adjacent marks"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): ChainReactionMod {
            return ChainReactionMod(
                threshold = if (boardSize >= 4) random.nextInt(3, 5) else 3,
                configuredBoardSize = boardSize,
                seed = random.nextLong()
            )
        }
    }
}
