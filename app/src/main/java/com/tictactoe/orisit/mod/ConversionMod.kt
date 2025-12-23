package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * Conversion Mod - Converts opponent marks to your marks under certain conditions.
 */
class ConversionMod(
    val conversionChance: Int = 20, // percentage
    val frequency: Int = 3,
    private val configuredBoardSize: Int = 3,
    private val seed: Long = System.currentTimeMillis()
) : BaseMod() {

    override val modId = "conversion"
    override val displayName = "Conversion"
    override val icon = "🔄"
    override val category = ModCategory.RULE_MUTATION
    override val description get() = "$conversionChance% chance to convert an opponent mark every $frequency turns"

    private var internalRandom = Random(seed)

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        // Check if conversion triggers
        if (internalRandom.nextInt(100) >= conversionChance) {
            return Pair(state, null)
        }
        
        val newBoard = convertMark(state.board, state.currentPlayer.opponent())
        if (newBoard == state.board) return Pair(state, null)
        
        val effect = ModEffect.RandomEffect(
            effectName = "Conversion",
            description = "A mark was converted!"
        )
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    private fun convertMark(board: Board, targetPlayer: Player): Board {
        val size = board.size
        val targets = mutableListOf<Cell>()
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val cell = Cell(row, col)
                if (board.get(row, col) == targetPlayer && !board.isDisabled(cell)) {
                    targets.add(cell)
                }
            }
        }
        
        if (targets.isEmpty()) return board
        
        val target = targets[internalRandom.nextInt(targets.size)]
        return board.set(target, targetPlayer.opponent())
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.RandomEffect("Conversion", "$conversionChance% chance to convert")
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "$conversionChance% conversion chance every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): ConversionMod {
            return ConversionMod(
                conversionChance = listOf(15, 20, 25, 30)[random.nextInt(4)],
                frequency = random.nextInt(2, 4),
                configuredBoardSize = boardSize,
                seed = random.nextLong()
            )
        }
    }
}
