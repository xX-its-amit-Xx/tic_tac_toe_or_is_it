package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.RotationRegion
import kotlin.random.Random

/**
 * MOD 1: BOARD ROTATION (Enhanced)
 * The entire board or regions rotate during play.
 */
class RotationMod(
    val frequency: Int,              // Every X turns (1, 2, or 3)
    val clockwise: Boolean,          // Direction
    val angle: Int,                  // 90 or 180 degrees
    val region: RotationRegion = RotationRegion.FULL  // Affected region
) : BaseMod() {

    override val modId = "rotation"
    override val displayName = "Dynamic Rotation"
    override val icon = "🔄"
    override val category = ModCategory.SPATIAL
    override val description: String
        get() {
            val dir = if (clockwise) "clockwise" else "counter-clockwise"
            val regionText = when (region) {
                RotationRegion.FULL -> "Board"
                RotationRegion.TOP_LEFT -> "Top-left quadrant"
                RotationRegion.TOP_RIGHT -> "Top-right quadrant"
                RotationRegion.BOTTOM_LEFT -> "Bottom-left quadrant"
                RotationRegion.BOTTOM_RIGHT -> "Bottom-right quadrant"
            }
            return "$regionText rotates $angle° $dir every $frequency turn(s)"
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

        val rotatedBoard = if (region == RotationRegion.FULL) {
            when {
                angle == 180 -> state.board.rotate180()
                clockwise -> state.board.rotate90Clockwise()
                else -> state.board.rotate90CounterClockwise()
            }
        } else {
            state.board.rotateRegion(region, clockwise)
        }

        rotationCount++
        val effect = ModEffect.Rotation(angle, clockwise, region)
        return Pair(state.copy(board = rotatedBoard, lastModEffect = effect), effect)
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated) return null
        val nextTurn = turnsSinceActivation(state) + 1
        if (nextTurn > 0 && nextTurn % frequency == 0) {
            return ModEffect.Rotation(angle, clockwise, region)
        }
        return null
    }

    override fun getParameterSummary(): String {
        val dir = if (clockwise) "CW" else "CCW"
        val regionText = if (region == RotationRegion.FULL) "" else " (${region.name})"
        return "Every $frequency turn(s), $angle° $dir$regionText"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): RotationMod {
            val usePartialRotation = boardSize >= 4 && random.nextFloat() < 0.3f
            val region = if (usePartialRotation) {
                RotationRegion.entries[random.nextInt(1, RotationRegion.entries.size)]
            } else {
                RotationRegion.FULL
            }
            
            return RotationMod(
                frequency = random.nextInt(1, 4),
                clockwise = random.nextBoolean(),
                angle = if (random.nextBoolean()) 90 else 180,
                region = region
            )
        }
    }
}
