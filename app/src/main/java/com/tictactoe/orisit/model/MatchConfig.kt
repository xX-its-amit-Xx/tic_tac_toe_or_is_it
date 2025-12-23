package com.tictactoe.orisit.model

import com.tictactoe.orisit.mod.IMod
import com.tictactoe.orisit.mod.ModFactory
import kotlin.random.Random

/**
 * Immutable configuration for a single match.
 * Stores the selected mod and all parameters.
 */
data class MatchConfig(
    val mod: IMod,
    val modActivationTurn: Int,
    val seed: Long,
    val humanPlayer: Player = Player.X
) {
    companion object {
        const val DEFAULT_ACTIVATION_TURN = 4

        /**
         * Generate a new match configuration with random mod and parameters.
         */
        fun generate(
            seed: Long = System.currentTimeMillis(),
            activationTurn: Int = DEFAULT_ACTIVATION_TURN
        ): MatchConfig {
            val random = Random(seed)
            val mod = ModFactory.createRandomMod(random)

            return MatchConfig(
                mod = mod,
                modActivationTurn = activationTurn,
                seed = seed
            )
        }

        /**
         * Generate a match with a specific mod type.
         */
        fun generateWithModType(
            modType: ModFactory.ModType,
            seed: Long = System.currentTimeMillis(),
            activationTurn: Int = DEFAULT_ACTIVATION_TURN
        ): MatchConfig {
            val random = Random(seed)
            val mod = ModFactory.createMod(modType, random)

            return MatchConfig(
                mod = mod,
                modActivationTurn = activationTurn,
                seed = seed
            )
        }
    }
}
