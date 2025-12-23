package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.BreakBehavior
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * TILE BEHAVIOR MOD: FRAGILE TILES
 * Tiles break after X uses.
 */
class FragileTilesMod(
    val durability: Int,                  // Uses before breaking (2 or 3)
    val breakBehavior: BreakBehavior,     // DISAPPEAR or LOCK
    private val seed: Long,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "fragile_tiles"
    override val displayName = "Fragile Tiles"
    override val icon = "💔"
    override val category = ModCategory.TILE_BEHAVIOR
    override val description: String
        get() {
            val behavior = when (breakBehavior) {
                BreakBehavior.DISAPPEAR -> "disappear"
                BreakBehavior.LOCK -> "lock"
            }
            return "Tiles $behavior after $durability uses"
        }

    private val fragileRandom = Random(seed)
    private var initialized = false

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        if (!initialized) {
            initialized = true
            val size = newState.board.size
            var board = newState.board
            
            // Set initial durability for some tiles
            val tileCount = (size * size) / 3
            val tiles = (0 until size * size).shuffled(fragileRandom).take(tileCount)
            for (idx in tiles) {
                val cell = Cell.fromIndex(idx, size)
                board = board.setDurability(cell, durability)
            }
            return newState.copy(board = board)
        }
        return newState
    }

    override fun onAfterMove(state: GameState, cell: Cell): GameState {
        if (!state.modActivated) return state
        
        val currentDurability = state.board.getDurability(cell)
        if (currentDurability <= 0) return state
        
        var newBoard = state.board.decrementDurability(cell)
        val newDurability = newBoard.getDurability(cell)
        
        if (newDurability <= 0) {
            newBoard = when (breakBehavior) {
                BreakBehavior.DISAPPEAR -> newBoard.set(cell, com.tictactoe.orisit.model.Player.NONE).disableCell(cell)
                BreakBehavior.LOCK -> newBoard.disableCell(cell)
            }
        }
        
        return state.copy(board = newBoard)
    }

    override fun shouldTrigger(state: GameState): Boolean = false

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        // Check for any tiles that broke this turn
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        val behavior = breakBehavior.name.lowercase()
        return "Durability: $durability, $behavior on break"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): FragileTilesMod {
            return FragileTilesMod(
                durability = random.nextInt(2, 4),
                breakBehavior = if (random.nextBoolean()) BreakBehavior.DISAPPEAR else BreakBehavior.LOCK,
                seed = random.nextLong(),
                configuredBoardSize = boardSize
            )
        }
    }
}
