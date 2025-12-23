package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * Mirror Mod - Mirrors the board horizontally or vertically at intervals.
 */
class MirrorMod(
    val mirrorAxis: MirrorAxis = MirrorAxis.HORIZONTAL,
    val frequency: Int = 2,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "mirror"
    override val displayName = "Mirror"
    override val icon = "🪞"
    override val category = ModCategory.SPATIAL
    override val description get() = "Board mirrors ${mirrorAxis.name.lowercase()} every $frequency turns"

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        val newBoard = mirrorBoard(state.board)
        val effect = ModEffect.Slide(
            isRow = mirrorAxis == MirrorAxis.HORIZONTAL,
            index = -1,
            direction = com.tictactoe.orisit.model.SlideDirection.LEFT
        )
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    private fun mirrorBoard(board: Board): Board {
        val size = board.size
        var newBoard = board
        
        when (mirrorAxis) {
            MirrorAxis.HORIZONTAL -> {
                for (row in 0 until size) {
                    for (col in 0 until size / 2) {
                        val mirrorCol = size - 1 - col
                        val temp = newBoard.get(row, col)
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(row, col), newBoard.get(row, mirrorCol))
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(row, mirrorCol), temp)
                    }
                }
            }
            MirrorAxis.VERTICAL -> {
                for (col in 0 until size) {
                    for (row in 0 until size / 2) {
                        val mirrorRow = size - 1 - row
                        val temp = newBoard.get(row, col)
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(row, col), newBoard.get(mirrorRow, col))
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(mirrorRow, col), temp)
                    }
                }
            }
            MirrorAxis.DIAGONAL -> {
                for (row in 0 until size) {
                    for (col in row + 1 until size) {
                        val temp = newBoard.get(row, col)
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(row, col), newBoard.get(col, row))
                        newBoard = newBoard.set(com.tictactoe.orisit.model.Cell(col, row), temp)
                    }
                }
            }
        }
        
        return newBoard
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.Slide(isRow = true, index = -1, direction = com.tictactoe.orisit.model.SlideDirection.LEFT)
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "${mirrorAxis.name.lowercase()} mirror every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): MirrorMod {
            return MirrorMod(
                mirrorAxis = MirrorAxis.entries[random.nextInt(MirrorAxis.entries.size)],
                frequency = random.nextInt(2, 4),
                configuredBoardSize = boardSize
            )
        }
    }
}

enum class MirrorAxis {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL
}
