package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlin.random.Random

/**
 * Teleport Mod - Randomly teleports marks to new positions.
 */
class TeleportMod(
    val teleportCount: Int = 1,
    val frequency: Int = 3,
    private val configuredBoardSize: Int = 3,
    private val seed: Long = System.currentTimeMillis()
) : BaseMod() {

    override val modId = "teleport"
    override val displayName = "Teleport"
    override val icon = "🌀"
    override val category = ModCategory.CONTROLLED_CHAOS
    override val description get() = "$teleportCount mark(s) teleport every $frequency turns"

    private var internalRandom = Random(seed)

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) return Pair(state, null)
        
        val newBoard = teleportMarks(state.board)
        val effect = ModEffect.RandomEffect(
            effectName = "Teleport",
            description = "$teleportCount mark(s) teleported!"
        )
        
        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    private fun teleportMarks(board: Board): Board {
        val size = board.size
        var newBoard = board
        
        // Find all occupied and empty cells
        val occupiedCells = mutableListOf<Pair<Cell, Player>>()
        val emptyCells = mutableListOf<Cell>()
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val cell = Cell(row, col)
                val player = board.get(row, col)
                if (player != Player.NONE && !board.isDisabled(cell)) {
                    occupiedCells.add(cell to player)
                } else if (player == Player.NONE && !board.isDisabled(cell)) {
                    emptyCells.add(cell)
                }
            }
        }
        
        if (occupiedCells.isEmpty() || emptyCells.isEmpty()) return board
        
        // Teleport random marks
        val toTeleport = occupiedCells.shuffled(internalRandom).take(minOf(teleportCount, occupiedCells.size, emptyCells.size))
        val destinations = emptyCells.shuffled(internalRandom).take(toTeleport.size)
        
        for (i in toTeleport.indices) {
            val (sourceCell, player) = toTeleport[i]
            val destCell = destinations[i]
            newBoard = newBoard.set(sourceCell, Player.NONE)
            newBoard = newBoard.set(destCell, player)
        }
        
        return newBoard
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.RandomEffect("Teleport", "$teleportCount mark(s) will teleport!")
        }
        return null
    }

    override fun getParameterSummary(): String {
        return "$teleportCount teleport(s) every $frequency turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): TeleportMod {
            return TeleportMod(
                teleportCount = random.nextInt(1, 3),
                frequency = random.nextInt(2, 4),
                configuredBoardSize = boardSize,
                seed = random.nextLong()
            )
        }
    }
}
