package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * BOARD EVOLUTION MOD: BOARD RESIZE
 * Board temporarily becomes larger or smaller.
 */
class BoardResizeMod(
    val newSize: Int,              // Target size (3 or 5 for 4x4 boards)
    val duration: Int,             // Turns at new size (-1 = permanent)
    private val configuredBoardSize: Int = 4
) : BaseMod() {

    override val modId = "board_resize"
    override val displayName = "Board Growth"
    override val icon = "📐"
    override val category = ModCategory.BOARD_EVOLUTION
    override val description: String
        get() {
            val action = if (newSize > configuredBoardSize) "grows to" else "shrinks to"
            val dur = if (duration < 0) "permanently" else "for $duration turns"
            return "Board $action ${newSize}×${newSize} $dur"
        }

    private var originalSize = 0
    private var turnsAtNewSize = 0
    private var resized = false

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        originalSize = state.board.size
        turnsAtNewSize = 0
        resized = false
        return newState
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        if (!resized) return true
        if (duration > 0 && turnsAtNewSize >= duration) return true
        return false
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        
        if (!resized) {
            resized = true
            turnsAtNewSize = 0
            
            val newWinCondition = if (newSize <= 3) 3 else newSize
            val newBoard = state.board.resize(newSize, newWinCondition)
            val effect = ModEffect.BoardResize(originalSize, newSize)
            
            return Pair(
                state.copy(
                    board = newBoard,
                    temporaryBoardSize = newSize,
                    lastModEffect = effect
                ),
                effect
            )
        }
        
        turnsAtNewSize++
        
        // Check if we should revert
        if (duration > 0 && turnsAtNewSize >= duration) {
            val revertedBoard = state.board.resize(originalSize, originalSize)
            val effect = ModEffect.BoardResize(newSize, originalSize)
            
            return Pair(
                state.copy(
                    board = revertedBoard,
                    temporaryBoardSize = null,
                    lastModEffect = effect
                ),
                effect
            )
        }
        
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        
        if (!resized) {
            return ModEffect.BoardResize(originalSize, newSize)
        }
        
        if (duration > 0 && turnsAtNewSize + 1 >= duration) {
            return ModEffect.BoardResize(newSize, originalSize)
        }
        
        return null
    }

    override fun getParameterSummary(): String {
        val dur = if (duration < 0) "permanent" else "$duration turns"
        return "Resize to ${newSize}×${newSize}, $dur"
    }

    override fun isCompatibleWithBoardSize(size: Int): Boolean = size >= 3

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 4): BoardResizeMod {
            val possibleSizes = when (boardSize) {
                3 -> listOf(4, 5)
                4 -> listOf(3, 5)
                5 -> listOf(3, 4)
                else -> listOf(3, 4, 5)
            }
            
            return BoardResizeMod(
                newSize = possibleSizes[random.nextInt(possibleSizes.size)],
                duration = if (random.nextBoolean()) -1 else random.nextInt(3, 6),
                configuredBoardSize = boardSize
            )
        }
    }
}
