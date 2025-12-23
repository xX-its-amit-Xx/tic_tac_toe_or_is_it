package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import kotlin.random.Random

/**
 * MOD 1: BOARD ROTATION
 * The entire board rotates during play.
 */
class RotationMod(
    val frequency: Int,          // Every X turns (1, 2, or 3)
    val clockwise: Boolean,      // Direction
    val angle: Int               // 90 or 180 degrees
) : BaseMod() {

    override val modId = "rotation"
    override val displayName = "Board Rotation"
    override val icon = "🔄"
    override val description: String
        get() {
            val dir = if (clockwise) "clockwise" else "counter-clockwise"
            return "Board rotates $angle° $dir every $frequency turn(s)"
        }

    private var rotationCount = 0

    override fun onActivate(state: GameState): GameState {
        rotationCount = 0
        return super.onActivate(state)
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        val turnsSince = turnsSinceActivation(state)
        return turnsSince > 0 && turnsSince % frequency == 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        if (!shouldTrigger(state)) {
            return Pair(state, null)
        }

        val rotatedBoard = when {
            angle == 180 -> state.board.rotate180()
            clockwise -> state.board.rotate90Clockwise()
            else -> state.board.rotate90CounterClockwise()
        }

        rotationCount++
        val effect = ModEffect.Rotation(angle, clockwise)
        return Pair(state.copy(board = rotatedBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.Rotation(angle, clockwise)
        }
        return null
    }

    override fun getParameterSummary(): String {
        val dir = if (clockwise) "CW" else "CCW"
        return "Every $frequency turn(s), $angle° $dir"
    }

    companion object {
        fun randomize(random: Random = Random): RotationMod {
            return RotationMod(
                frequency = random.nextInt(1, 4),      // 1, 2, or 3
                clockwise = random.nextBoolean(),
                angle = if (random.nextBoolean()) 90 else 180
            )
        }
    }
}
