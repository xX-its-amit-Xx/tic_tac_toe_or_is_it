package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * Clone Mod - Occasionally clones a random mark to an adjacent empty cell.
 */
class CloneMod(
    val cloneChance: Int = 30, // percentage
    val frequency: Int = 2,
    private val configuredBoardSize: Int = 3,
    private val seed: Long = System.currentTimeMillis()
) : BaseMod() {

    override val modId = "clone"
    override val displayName = "Clone"
    override val icon = "👯"
    override val category = ModCategory.CONTROLLED_CHAOS
    override val description get() = "$cloneChance% chance to clone a mark every $frequency turns"

    private var internalRandom = Random(seed)

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        // Check if clone triggers
        if (internalRandom.nextInt(100) >= cloneChance) {
            return Pair(state, null)
        }
        
        val newBoard = cloneMark(state.board)
        if (newBoard == state.board) return Pair(state, null)
        
        val effect = ModEffect.RandomEffect(
            effectName = "Clone",
            description = "A mark was cloned!"
        )
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    private fun cloneMark(board: Board): Board {
        val size = board.size
        
        // Find all occupied cells with adjacent empty cells
        val candidates = mutableListOf<Triple<Cell, Player, Cell>>() // source, player, destination
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val cell = Cell(row, col)
                val player = board.get(row, col)
                if (player != Player.NONE && !board.isDisabled(cell)) {
                    // Find adjacent empty cells
                    val adjacents = listOf(
                        Cell(row - 1, col),
                        Cell(row + 1, col),
                        Cell(row, col - 1),
                        Cell(row, col + 1)
                    ).filter { it.isValid(size) && 
                              board.get(it.row, it.col) == Player.NONE && 
                              !board.isDisabled(it) }
                    
                    for (adj in adjacents) {
                        candidates.add(Triple(cell, player, adj))
                    }
                }
            }
        }
        
        if (candidates.isEmpty()) return board
        
        val (_, player, dest) = candidates[internalRandom.nextInt(candidates.size)]
        return board.set(dest, player)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.RandomEffect("Clone", "$cloneChance% chance to clone")
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "$cloneChance% clone chance every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): CloneMod {
            return CloneMod(
                cloneChance = listOf(20, 30, 40, 50)[random.nextInt(4)],
                frequency = random.nextInt(1, 3),
                configuredBoardSize = boardSize,
                seed = random.nextLong()
            )
        }
    }
}
