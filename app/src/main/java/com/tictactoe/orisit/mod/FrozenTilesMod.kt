package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * TILE BEHAVIOR MOD: FROZEN TILES
 * Tiles become temporarily unplayable.
 */
class FrozenTilesMod(
    val freezeDuration: Int,       // Turns frozen (2 or 3)
    val freezePattern: FreezePattern,
    val freezeInterval: Int,       // New freeze every X turns
    private val seed: Long,
    private val configuredBoardSize: Int = 3
) : BaseMod() {

    override val modId = "frozen_tiles"
    override val displayName = "Frozen Tiles"
    override val icon = "❄️"
    override val category = ModCategory.TILE_BEHAVIOR
    override val description: String
        get() {
            val pattern = freezePattern.name.lowercase().replace('_', ' ')
            return "Tiles freeze in $pattern pattern for $freezeDuration turns"
        }

    private val freezeRandom = Random(seed)
    private val frozenTurns = mutableMapOf<Int, Int>() // cellIndex -> turnsRemaining

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % freezeInterval == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        var newBoard = state.board
        val size = state.board.size
        
        // Decrement frozen counters and unfreeze expired tiles
        val toRemove = mutableListOf<Int>()
        for ((idx, turns) in frozenTurns) {
            if (turns <= 1) {
                toRemove.add(idx)
                newBoard = newBoard.unfreezeCell(Cell.fromIndex(idx, size))
            } else {
                frozenTurns[idx] = turns - 1
            }
        }
        toRemove.forEach { frozenTurns.remove(it) }
        
        // Apply new freezes if triggered
        if (shouldTrigger(state)) {
            val cellsToFreeze = selectCellsToFreeze(state)
            for (idx in cellsToFreeze) {
                if (!frozenTurns.containsKey(idx)) {
                    frozenTurns[idx] = freezeDuration
                    newBoard = newBoard.freezeCell(Cell.fromIndex(idx, size))
                }
            }
            
            val effect = ModEffect.TileFreeze(cellsToFreeze, freezeDuration)
            return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
        }
        
        return Pair(state.copy(board = newBoard), null)
    }

    private fun selectCellsToFreeze(state: GameState): List<Int> {
        val size = state.board.size
        val totalCells = size * size
        
        return when (freezePattern) {
            FreezePattern.RANDOM -> {
                val count = maxOf(1, size / 2)
                (0 until totalCells)
                    .filter { state.board.isEmpty(Cell.fromIndex(it, size)) }
                    .shuffled(freezeRandom)
                    .take(count)
            }
            FreezePattern.CORNERS -> {
                listOf(0, size - 1, totalCells - size, totalCells - 1)
                    .filter { state.board.isEmpty(Cell.fromIndex(it, size)) }
            }
            FreezePattern.EDGES -> {
                val edges = mutableListOf<Int>()
                for (i in 0 until size) {
                    edges.add(i) // top
                    edges.add(totalCells - 1 - i) // bottom
                    if (i > 0 && i < size - 1) {
                        edges.add(i * size) // left
                        edges.add(i * size + size - 1) // right
                    }
                }
                edges.filter { state.board.isEmpty(Cell.fromIndex(it, size)) }
                    .shuffled(freezeRandom).take(size)
            }
            FreezePattern.CENTER -> {
                val center = size / 2
                val centerCells = mutableListOf<Int>()
                for (r in (center - 1)..(center)) {
                    for (c in (center - 1)..(center)) {
                        if (r in 0 until size && c in 0 until size) {
                            centerCells.add(r * size + c)
                        }
                    }
                }
                centerCells.filter { state.board.isEmpty(Cell.fromIndex(it, size)) }
            }
        }
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % freezeInterval == 0) {
            return ModEffect.TileFreeze(emptyList(), freezeDuration)
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "${freezePattern.name}, $freezeDuration turns, every $freezeInterval"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): FrozenTilesMod {
            return FrozenTilesMod(
                freezeDuration = random.nextInt(2, 4),
                freezePattern = FreezePattern.entries[random.nextInt(FreezePattern.entries.size)],
                freezeInterval = random.nextInt(2, 4),
                seed = random.nextLong(),
                configuredBoardSize = boardSize
            )
        }
    }
}

enum class FreezePattern {
    RANDOM,
    CORNERS,
    EDGES,
    CENTER
}
