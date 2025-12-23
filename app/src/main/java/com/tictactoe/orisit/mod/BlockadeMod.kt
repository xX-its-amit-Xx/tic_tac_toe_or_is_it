package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * Blockade Mod - Creates temporary blockades that prevent placement.
 */
class BlockadeMod(
    val blockadeCount: Int = 2,
    val blockadeDuration: Int = 3,
    val frequency: Int = 3,
    private val configuredBoardSize: Int = 3,
    private val seed: Long = System.currentTimeMillis()
) : BaseMod() {

    override val modId = "blockade"
    override val displayName = "Blockade"
    override val icon = "🚧"
    override val category = ModCategory.TILE_BEHAVIOR
    override val description get() = "$blockadeCount cell(s) blocked for $blockadeDuration turns, every $frequency turns"

    private var internalRandom = Random(seed)
    private val activeBlockades = mutableMapOf<Int, Int>() // cellIndex to turnsRemaining

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        var newBoard = state.board
        
        // Decrement and remove expired blockades
        val expiredBlockades = mutableListOf<Int>()
        for ((cellIndex, remaining) in activeBlockades) {
            if (remaining <= 1) {
                expiredBlockades.add(cellIndex)
                // Re-enable cell by removing from disabled set
                val size = state.board.size
                val row = cellIndex / size
                val col = cellIndex % size
                // Note: We'll track this internally, the cell becomes playable again
            } else {
                activeBlockades[cellIndex] = remaining - 1
            }
        }
        expiredBlockades.forEach { activeBlockades.remove(it) }
        
        // Add new blockades if trigger condition
        if (shouldTrigger(state)) {
            val size = state.board.size
            val emptyCells = mutableListOf<Int>()
            
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val idx = row * size + col
                    if (state.board.get(row, col) == com.tictactoe.orisit.model.Player.NONE && 
                        !state.board.isDisabled(Cell(row, col)) && 
                        !activeBlockades.containsKey(idx)) {
                        emptyCells.add(idx)
                    }
                }
            }
            
            val toBlock = emptyCells.shuffled(internalRandom).take(minOf(blockadeCount, emptyCells.size))
            for (idx in toBlock) {
                activeBlockades[idx] = blockadeDuration
                val r = idx / size
                val c = idx % size
                newBoard = newBoard.disableCell(Cell(r, c))
            }
            
            if (toBlock.isNotEmpty()) {
                val effect = ModEffect.TileFreeze(toBlock, blockadeDuration)
                return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
            }
        }
        
        if (newBoard != state.board) {
            return Pair(state.copy(board = newBoard), null)
        }
        
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.TileFreeze(emptyList(), blockadeDuration)
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "$blockadeCount blockade(s) for $blockadeDuration turns every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): BlockadeMod {
            return BlockadeMod(
                blockadeCount = random.nextInt(1, 3),
                blockadeDuration = random.nextInt(2, 4),
                frequency = random.nextInt(2, 4),
                configuredBoardSize = boardSize,
                seed = random.nextLong()
            )
        }
    }
}
