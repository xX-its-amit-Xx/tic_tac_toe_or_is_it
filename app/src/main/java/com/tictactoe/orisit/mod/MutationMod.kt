package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.MutationPreview
import com.tictactoe.orisit.model.MutationType
import kotlin.random.Random

/**
 * MOD 3: BOARD MUTATION
 * The board layout itself changes.
 */
class MutationMod(
    val mutationType: MutationType,  // ROW_SWAP, COLUMN_SWAP, TILE_REMOVAL
    val frequency: Int,               // Every X turns (2 or 3)
    val count: Int,                   // How many affected (1 or 2)
    private val seed: Long            // For deterministic mutations
) : BaseMod() {

    override val modId = "mutation"
    override val displayName = "Board Mutation"
    override val icon = "🧩"
    override val description: String
        get() {
            val type = when (mutationType) {
                MutationType.ROW_SWAP -> "rows swap"
                MutationType.COLUMN_SWAP -> "columns swap"
                MutationType.TILE_REMOVAL -> "tiles removed"
            }
            return "$count $type every $frequency turns"
        }

    private val mutationRandom = Random(seed)
    private var pendingMutation: MutationData? = null
    private var mutationScheduledFor: Int = -1

    data class MutationData(
        val type: MutationType,
        val indices: List<Int>  // Row/col indices or cell indices
    )

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        scheduleMutation(newState.turnCount + frequency)
        return newState
    }

    private fun scheduleMutation(forTurn: Int) {
        mutationScheduledFor = forTurn
        pendingMutation = generateMutation()
    }

    private fun generateMutation(): MutationData {
        return when (mutationType) {
            MutationType.ROW_SWAP -> {
                val rows = (0..2).shuffled(mutationRandom).take(2)
                MutationData(mutationType, rows)
            }
            MutationType.COLUMN_SWAP -> {
                val cols = (0..2).shuffled(mutationRandom).take(2)
                MutationData(mutationType, cols)
            }
            MutationType.TILE_REMOVAL -> {
                val cells = (0..8).shuffled(mutationRandom).take(count)
                MutationData(mutationType, cells)
            }
        }
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return state.turnCount >= mutationScheduledFor && mutationScheduledFor > 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            val preview = previewNextEffect(state)
            if (preview != null && pendingMutation != null) {
                val turnsUntil = mutationScheduledFor - state.turnCount
                if (turnsUntil == 1) {
                    return Pair(
                        state.copy(
                            pendingMutation = MutationPreview(
                                pendingMutation!!.type,
                                pendingMutation!!.indices,
                                turnsUntil
                            )
                        ),
                        null
                    )
                }
            }
            return Pair(state, null)
        }

        val mutation = pendingMutation ?: return Pair(state, null)
        val newBoard = applyMutation(state.board, mutation)
        val effect = ModEffect.Mutation(mutation.type, mutation.indices)

        scheduleMutation(state.turnCount + frequency)

        return Pair(
            state.copy(
                board = newBoard,
                lastModEffect = effect,
                pendingMutation = null
            ),
            effect
        )
    }

    private fun applyMutation(board: Board, mutation: MutationData): Board {
        return when (mutation.type) {
            MutationType.ROW_SWAP -> {
                if (mutation.indices.size >= 2) {
                    board.swapRows(mutation.indices[0], mutation.indices[1])
                } else board
            }
            MutationType.COLUMN_SWAP -> {
                if (mutation.indices.size >= 2) {
                    board.swapColumns(mutation.indices[0], mutation.indices[1])
                } else board
            }
            MutationType.TILE_REMOVAL -> {
                var newBoard = board
                mutation.indices.forEach { idx ->
                    newBoard = newBoard.disableCell(Cell.fromIndex(idx))
                }
                newBoard
            }
        }
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated || pendingMutation == null) return null
        val turnsUntil = mutationScheduledFor - state.turnCount
        if (turnsUntil in 1..2) {
            return ModEffect.Mutation(pendingMutation!!.type, pendingMutation!!.indices)
        }
        return null
    }

    override fun getParameterSummary(): String {
        val type = when (mutationType) {
            MutationType.ROW_SWAP -> "Row Swap"
            MutationType.COLUMN_SWAP -> "Col Swap"
            MutationType.TILE_REMOVAL -> "Tile Remove"
        }
        return "$type ($count) every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random): MutationMod {
            return MutationMod(
                mutationType = MutationType.entries[random.nextInt(3)],
                frequency = random.nextInt(2, 4),  // 2 or 3
                count = random.nextInt(1, 3),       // 1 or 2
                seed = random.nextLong()
            )
        }
    }
}
