package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.SlideDirection
import kotlin.random.Random

/**
 * SPATIAL MOD: SLIDING ROWS
 * Entire rows or columns slide cyclically.
 */
class SlidingRowsMod(
    val rowCount: Int,              // Number of rows/cols to slide (1 or 2)
    val isRow: Boolean,             // true = rows, false = columns
    val direction: SlideDirection,  // Direction of slide
    val interval: Int,              // Triggers every X turns
    private val seed: Long
) : BaseMod() {

    override val modId = "sliding_rows"
    override val displayName = "Sliding Rows"
    override val icon = "↔️"
    override val category = ModCategory.SPATIAL
    override val description: String
        get() {
            val target = if (isRow) "row(s)" else "column(s)"
            val dir = direction.name.lowercase()
            return "$rowCount $target slide $dir every $interval turn(s)"
        }

    private val slideRandom = Random(seed)
    private var lastSlideIndices = listOf<Int>()

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % interval == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            return Pair(state, null)
        }

        val size = state.board.size
        val indices = (0 until size).shuffled(slideRandom).take(rowCount)
        lastSlideIndices = indices

        var newBoard = state.board
        for (idx in indices) {
            newBoard = if (isRow) {
                newBoard.slideRow(idx, direction)
            } else {
                newBoard.slideColumn(idx, direction)
            }
        }

        val effect = ModEffect.Slide(isRow, indices.first(), direction)
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % interval == 0) {
            return ModEffect.Slide(isRow, 0, direction)
        }
        return null
    }

    override fun getParameterSummary(): String {
        val target = if (isRow) "rows" else "cols"
        return "$rowCount $target, ${direction.name}, every $interval turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): SlidingRowsMod {
            val direction = if (random.nextBoolean()) {
                if (random.nextBoolean()) SlideDirection.LEFT else SlideDirection.RIGHT
            } else {
                if (random.nextBoolean()) SlideDirection.UP else SlideDirection.DOWN
            }
            val isRow = direction == SlideDirection.LEFT || direction == SlideDirection.RIGHT
            
            return SlidingRowsMod(
                rowCount = random.nextInt(1, 3),
                isRow = isRow,
                direction = direction,
                interval = random.nextInt(1, 4),
                seed = random.nextLong()
            )
        }
    }
}
