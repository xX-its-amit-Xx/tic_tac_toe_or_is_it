package com.tictactoe.orisit.mod

import kotlin.random.Random

/**
 * Factory for creating randomized mods with their parameters.
 */
object ModFactory {

    enum class ModType {
        ROTATION,
        GRAVITY,
        MUTATION
    }

    /**
     * Create a random mod with randomized parameters.
     */
    fun createRandomMod(random: Random = Random): IMod {
        return when (ModType.entries[random.nextInt(3)]) {
            ModType.ROTATION -> RotationMod.randomize(random)
            ModType.GRAVITY -> GravityMod.randomize(random)
            ModType.MUTATION -> MutationMod.randomize(random)
        }
    }

    /**
     * Create a specific mod type with randomized parameters.
     */
    fun createMod(type: ModType, random: Random = Random): IMod {
        return when (type) {
            ModType.ROTATION -> RotationMod.randomize(random)
            ModType.GRAVITY -> GravityMod.randomize(random)
            ModType.MUTATION -> MutationMod.randomize(random)
        }
    }

    /**
     * Get all available mod types.
     */
    fun getAvailableModTypes(): List<ModType> = ModType.entries
}
