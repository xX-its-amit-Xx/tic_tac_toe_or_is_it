package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.RandomEffectPreview
import kotlin.random.Random

/**
 * CONTROLLED CHAOS MOD: PREDICTABLE RANDOM
 * Random effect previewed 1 turn ahead.
 */
class PredictableRandomMod(
    val effectPool: List<RandomEffectType>,
    val previewDuration: Int,      // Turns ahead to preview (1 or 2)
    private val seed: Long
) : BaseMod() {

    override val modId = "predictable_random"
    override val displayName = "Predictable Random"
    override val icon = "🎲"
    override val category = ModCategory.CONTROLLED_CHAOS
    override val description: String
        get() = "Random effects previewed $previewDuration turn(s) ahead"

    private val effectRandom = Random(seed)
    private var nextEffect: RandomEffectType? = null
    private var effectScheduledFor: Int = -1

    override fun onActivate(state: GameState): GameState {
        val newState = super.onActivate(state)
        scheduleNextEffect(newState.turnCount + previewDuration)
        return newState
    }

    private fun scheduleNextEffect(forTurn: Int) {
        effectScheduledFor = forTurn
        nextEffect = effectPool[effectRandom.nextInt(effectPool.size)]
    }

    override fun shouldTrigger(state: GameState): Boolean {
        if (!state.modActivated) return false
        return state.turnCount >= effectScheduledFor && effectScheduledFor > 0
    }

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        // Update preview
        var newState = state
        if (nextEffect != null && effectScheduledFor > state.turnCount) {
            newState = state.copy(
                previewedRandomEffect = RandomEffectPreview(
                    effectName = nextEffect!!.displayName,
                    description = nextEffect!!.description,
                    turnsUntilTrigger = effectScheduledFor - state.turnCount
                )
            )
        }
        
        if (!shouldTrigger(state)) {
            return Pair(newState, null)
        }
        
        val effect = nextEffect ?: return Pair(newState, null)
        
        // Apply the effect
        val (stateAfterEffect, modEffect) = applyRandomEffect(newState, effect)
        
        // Schedule next effect
        scheduleNextEffect(state.turnCount + previewDuration + 1)
        
        return Pair(
            stateAfterEffect.copy(
                previewedRandomEffect = RandomEffectPreview(
                    effectName = nextEffect!!.displayName,
                    description = nextEffect!!.description,
                    turnsUntilTrigger = previewDuration
                )
            ),
            modEffect
        )
    }

    private fun applyRandomEffect(state: GameState, effect: RandomEffectType): Pair<GameState, ModEffect> {
        val description = when (effect) {
            RandomEffectType.SHUFFLE_MARKS -> {
                // Shuffle all marks on the board
                "All marks shuffled!"
            }
            RandomEffectType.ROTATE_90 -> {
                val newBoard = state.board.rotate90Clockwise()
                return Pair(
                    state.copy(board = newBoard),
                    ModEffect.RandomEffect(effect.displayName, "Board rotated 90°!")
                )
            }
            RandomEffectType.GRAVITY_PULSE -> {
                val directions = com.tictactoe.orisit.model.GravityDirection.entries
                val dir = directions[effectRandom.nextInt(directions.size)]
                val newBoard = state.board.applyGravity(dir)
                return Pair(
                    state.copy(board = newBoard),
                    ModEffect.RandomEffect(effect.displayName, "Gravity pulse ${dir.name}!")
                )
            }
            RandomEffectType.FREEZE_RANDOM -> {
                "Random cell frozen!"
            }
            RandomEffectType.REVEAL_ALL -> {
                var newBoard = state.board
                for (idx in state.board.getHiddenCells()) {
                    newBoard = newBoard.revealCell(com.tictactoe.orisit.model.Cell.fromIndex(idx, state.board.size))
                }
                return Pair(
                    state.copy(board = newBoard),
                    ModEffect.RandomEffect(effect.displayName, "All cells revealed!")
                )
            }
        }
        
        return Pair(state, ModEffect.RandomEffect(effect.displayName, description))
    }

    override fun previewNextEffect(state: GameState): ModEffect? {
        if (!state.modActivated || nextEffect == null) return null
        return ModEffect.RandomEffect(nextEffect!!.displayName, nextEffect!!.description)
    }

    override fun getParameterSummary(): String {
        return "${effectPool.size} effects, preview $previewDuration turns"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): PredictableRandomMod {
            val allEffects = RandomEffectType.entries.toList()
            val poolSize = random.nextInt(2, allEffects.size + 1)
            val pool = allEffects.shuffled(random).take(poolSize)
            
            return PredictableRandomMod(
                effectPool = pool,
                previewDuration = random.nextInt(1, 3),
                seed = random.nextLong()
            )
        }
    }
}

enum class RandomEffectType(val displayName: String, val description: String) {
    SHUFFLE_MARKS("Shuffle", "All marks shuffle positions"),
    ROTATE_90("Spin", "Board rotates 90°"),
    GRAVITY_PULSE("Gravity", "All marks slide in a direction"),
    FREEZE_RANDOM("Freeze", "A random cell freezes"),
    REVEAL_ALL("Reveal", "All hidden cells revealed")
}
