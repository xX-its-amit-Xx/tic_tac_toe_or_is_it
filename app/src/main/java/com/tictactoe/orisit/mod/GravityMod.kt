package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.GravityDirection
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * MOD 2: GRAVITY SHIFT
 * Marks fall in a direction after placement.
 */
class GravityMod(
    val direction: GravityDirection,  // DOWN, UP, LEFT, RIGHT
    val strength: Int,                 // Apply every X turns (1 or 2)
    val lockAfterFall: Boolean         // Whether marks settle permanently
) : BaseMod() {

    override val modId = "gravity"
    override val displayName = "Gravity Shift"
    override val icon = "⬇️"
    override val category = ModCategory.SPATIAL
    override val description: String
        get() {
            val freq = if (strength == 1) "every turn" else "every 2 turns"
            val lock = if (lockAfterFall) " (marks lock)" else ""
            return "Gravity pulls ${direction.name.lowercase()} $freq$lock"
        }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince >= 0 && (turnsSince % strength == 0)
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            return Pair(state, null)
        }

        val newBoard = state.board.applyGravity(direction)
        val effect = ModEffect.Gravity(direction)

        return Pair(state.copy(board = newBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn >= 0 && (nextTurn % strength == 0)) {
            return ModEffect.Gravity(direction)
        }
        return null
    }

    override fun getParameterSummary(): String {
        val freq = if (strength == 1) "every turn" else "every 2 turns"
        val lock = if (lockAfterFall) ", locked" else ""
        return "${direction.symbol()} ${direction.name} $freq$lock"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): GravityMod {
            return GravityMod(
                direction = GravityDirection.entries[random.nextInt(4)],
                strength = random.nextInt(1, 3),
                lockAfterFall = random.nextBoolean()
            )
        }
    }
}
