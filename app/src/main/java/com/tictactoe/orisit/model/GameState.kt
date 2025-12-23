package com.tictactoe.orisit.model

data class GameState(
    val board: Board = Board(),
    val currentPlayer: Player = Player.X,
    val turnCount: Int = 0,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winner: Player = Player.NONE,
    val modActivated: Boolean = false,
    val lastModEffect: ModEffect? = null,
    val pendingMutation: MutationPreview? = null,
    val alternateWinCondition: AlternateWinCondition? = null,
    val doubleTurnActive: Boolean = false,
    val playerAbilities: Map<Player, PlayerAbility> = emptyMap(),
    val previewedRandomEffect: RandomEffectPreview? = null,
    val temporaryBoardSize: Int? = null,
    val activeSubMod: SubModEffect? = null
) {
    fun isGameOver(): Boolean = status != GameStatus.IN_PROGRESS

    fun switchPlayer(): GameState = if (doubleTurnActive) {
        copy(doubleTurnActive = false)
    } else {
        copy(currentPlayer = currentPlayer.opponent())
    }

    fun incrementTurn(): GameState = copy(turnCount = turnCount + 1)

    fun setWinner(player: Player): GameState = copy(
        winner = player,
        status = if (player == Player.NONE) GameStatus.DRAW else GameStatus.WON
    )

    fun activateMod(): GameState = copy(modActivated = true)

    fun enableDoubleTurn(): GameState = copy(doubleTurnActive = true)

    fun setAlternateWinCondition(condition: AlternateWinCondition?): GameState = 
        copy(alternateWinCondition = condition)

    fun setPlayerAbility(player: Player, ability: PlayerAbility): GameState {
        val newAbilities = playerAbilities.toMutableMap()
        newAbilities[player] = ability
        return copy(playerAbilities = newAbilities)
    }

    fun usePlayerAbility(player: Player): GameState {
        val ability = playerAbilities[player] ?: return this
        if (ability.usesRemaining <= 0) return this
        val newAbilities = playerAbilities.toMutableMap()
        newAbilities[player] = ability.copy(usesRemaining = ability.usesRemaining - 1)
        return copy(playerAbilities = newAbilities)
    }

    fun getEffectiveWinCondition(): Int {
        return alternateWinCondition?.requiredInRow ?: board.winCondition
    }

    fun shouldCheckSquareWin(): Boolean = alternateWinCondition?.squarePatternWins == true
}

enum class GameStatus {
    IN_PROGRESS,
    WON,
    DRAW
}

sealed class ModEffect {
    data class Rotation(
        val angle: Int,
        val clockwise: Boolean,
        val region: RotationRegion = RotationRegion.FULL
    ) : ModEffect()

    data class Gravity(
        val direction: GravityDirection
    ) : ModEffect()

    data class Mutation(
        val type: MutationType,
        val affectedIndices: List<Int>
    ) : ModEffect()

    data class Slide(
        val isRow: Boolean,
        val index: Int,
        val direction: SlideDirection
    ) : ModEffect()

    data class TileBreak(
        val cellIndex: Int,
        val behavior: BreakBehavior
    ) : ModEffect()

    data class TileFreeze(
        val cellIndices: List<Int>,
        val duration: Int
    ) : ModEffect()

    data class TrapTriggered(
        val cellIndex: Int,
        val trapType: TrapType,
        val resultDescription: String
    ) : ModEffect()

    data class MoveDecay(
        val decayedCellIndices: List<Int>
    ) : ModEffect()

    data class DoubleTurn(
        val player: Player
    ) : ModEffect()

    data class FogUpdate(
        val hiddenCells: Set<Int>,
        val revealedCells: Set<Int>
    ) : ModEffect()

    data class DelayedReveal(
        val cell: Cell,
        val player: Player
    ) : ModEffect()

    data class WinConditionChange(
        val newCondition: AlternateWinCondition
    ) : ModEffect()

    data class AbilityUsed(
        val player: Player,
        val abilityType: AbilityType,
        val targetCell: Cell?
    ) : ModEffect()

    data class RandomEffect(
        val effectName: String,
        val description: String
    ) : ModEffect()

    data class BoardResize(
        val oldSize: Int,
        val newSize: Int
    ) : ModEffect()

    data class TileDrift(
        val direction: DriftDirection,
        val affectedCells: List<Int>
    ) : ModEffect()

    data class SubModActivated(
        val modName: String,
        val duration: Int
    ) : ModEffect()
}

data class MutationPreview(
    val type: MutationType,
    val affectedIndices: List<Int>,
    val turnsUntilActivation: Int
)

enum class MutationType {
    ROW_SWAP,
    COLUMN_SWAP,
    TILE_REMOVAL
}

enum class BreakBehavior {
    DISAPPEAR,
    LOCK
}

data class AlternateWinCondition(
    val requiredInRow: Int,
    val squarePatternWins: Boolean = false,
    val duration: Int = -1,
    val turnsRemaining: Int = -1
)

data class PlayerAbility(
    val type: AbilityType,
    val usesRemaining: Int,
    val totalUses: Int
)

enum class AbilityType {
    EXTRA_MOVE,
    REMOVE_OPPONENT_MARK,
    SWAP_ANY_MARKS,
    FREEZE_CELL,
    REVEAL_HIDDEN,
    BLOCK_MOD_EFFECT
}

data class RandomEffectPreview(
    val effectName: String,
    val description: String,
    val turnsUntilTrigger: Int
)

data class SubModEffect(
    val modName: String,
    val turnsRemaining: Int
)
