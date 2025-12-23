package com.tictactoe.orisit.model

import com.tictactoe.orisit.mod.IMod
import com.tictactoe.orisit.mod.ModFactory
import kotlin.random.Random

/**
 * Immutable configuration for a single match.
 * Stores the selected mod, board settings, and all parameters.
 */
data class MatchConfig(
    val mod: IMod,
    val modActivationTurn: Int,
    val seed: Long,
    val humanPlayer: Player = Player.X,
    val boardSize: Int = 3,
    val winCondition: Int = boardSize,
    val gameMode: GameMode = GameMode.VS_AI,
    val aiDifficulty: AIDifficulty = AIDifficulty.NORMAL,
    val modPool: ModPool = ModPool.NORMAL,
    val stackedMods: List<IMod> = emptyList() // Additional mods that stack with the primary mod
) {
    // Get all active mods (primary + stacked)
    fun getAllMods(): List<IMod> = listOf(mod) + stackedMods
    companion object {
        const val DEFAULT_ACTIVATION_TURN_3X3 = 4
        const val DEFAULT_ACTIVATION_TURN_4X4 = 5

        fun getDefaultActivationTurn(boardSize: Int): Int = when (boardSize) {
            3 -> DEFAULT_ACTIVATION_TURN_3X3
            4 -> DEFAULT_ACTIVATION_TURN_4X4
            else -> boardSize + 1
        }

        /**
         * Generate a new match configuration with random mod and parameters.
         */
        fun generate(
            seed: Long = System.currentTimeMillis(),
            boardSize: Int = 3,
            winCondition: Int = boardSize,
            activationTurn: Int = getDefaultActivationTurn(boardSize),
            gameMode: GameMode = GameMode.VS_AI,
            aiDifficulty: AIDifficulty = AIDifficulty.NORMAL,
            modPool: ModPool = ModPool.NORMAL,
            modCount: Int = 1 // How many mods to stack
        ): MatchConfig {
            val random = Random(seed)
            
            // For stacking, create multiple unique mods
            val allMods = if (modCount > 1) {
                ModFactory.createStackedMods(modCount, random, boardSize, modPool)
            } else {
                listOf(ModFactory.createRandomMod(random, boardSize, modPool))
            }
            
            val primaryMod = allMods.first()
            val additionalMods = allMods.drop(1)

            return MatchConfig(
                mod = primaryMod,
                modActivationTurn = activationTurn,
                seed = seed,
                boardSize = boardSize,
                winCondition = winCondition,
                gameMode = gameMode,
                aiDifficulty = aiDifficulty,
                modPool = modPool,
                stackedMods = additionalMods
            )
        }

        /**
         * Generate a match with a specific mod type.
         */
        fun generateWithModType(
            modType: ModFactory.ModType,
            seed: Long = System.currentTimeMillis(),
            boardSize: Int = 3,
            winCondition: Int = boardSize,
            activationTurn: Int = getDefaultActivationTurn(boardSize),
            gameMode: GameMode = GameMode.VS_AI,
            aiDifficulty: AIDifficulty = AIDifficulty.NORMAL
        ): MatchConfig {
            val random = Random(seed)
            val mod = ModFactory.createMod(modType, random, boardSize)

            return MatchConfig(
                mod = mod,
                modActivationTurn = activationTurn,
                seed = seed,
                boardSize = boardSize,
                winCondition = winCondition,
                gameMode = gameMode,
                aiDifficulty = aiDifficulty
            )
        }

        /**
         * Generate a match for Bluetooth multiplayer.
         */
        fun generateForBluetooth(
            seed: Long = System.currentTimeMillis(),
            boardSize: Int = 4,
            winCondition: Int = boardSize,
            modPool: ModPool = ModPool.NORMAL,
            isHost: Boolean = true,
            modCount: Int = 1
        ): MatchConfig {
            val random = Random(seed)
            
            val allMods = if (modCount > 1) {
                ModFactory.createStackedMods(modCount, random, boardSize, modPool)
            } else {
                listOf(ModFactory.createRandomMod(random, boardSize, modPool))
            }
            
            val primaryMod = allMods.first()
            val additionalMods = allMods.drop(1)

            return MatchConfig(
                mod = primaryMod,
                modActivationTurn = getDefaultActivationTurn(boardSize),
                seed = seed,
                humanPlayer = if (isHost) Player.X else Player.O,
                boardSize = boardSize,
                winCondition = winCondition,
                gameMode = GameMode.BLUETOOTH_MULTIPLAYER,
                modPool = modPool,
                stackedMods = additionalMods
            )
        }
    }

    fun createInitialBoard(): Board = Board.create(boardSize, winCondition)
}

enum class GameMode {
    VS_AI,
    LOCAL_MULTIPLAYER,
    BLUETOOTH_MULTIPLAYER
}

enum class AIDifficulty {
    EASY,
    NORMAL
}

enum class ModPool {
    NORMAL,
    CHAOS,
    CUSTOM
}
