package com.tictactoe.orisit.model

data class GameState(
    val board: Board = Board(),
    val currentPlayer: Player = Player.X,
    val turnCount: Int = 0,
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val winner: Player = Player.NONE,
    val modActivated: Boolean = false,
    val lastModEffect: ModEffect? = null,
    val pendingMutation: MutationPreview? = null
) {
    fun isGameOver(): Boolean = status != GameStatus.IN_PROGRESS

    fun switchPlayer(): GameState = copy(currentPlayer = currentPlayer.opponent())

    fun incrementTurn(): GameState = copy(turnCount = turnCount + 1)

    fun setWinner(player: Player): GameState = copy(
        winner = player,
        status = if (player == Player.NONE) GameStatus.DRAW else GameStatus.WON
    )

    fun activateMod(): GameState = copy(modActivated = true)
}

enum class GameStatus {
    IN_PROGRESS,
    WON,
    DRAW
}

sealed class ModEffect {
    data class Rotation(
        val angle: Int,
        val clockwise: Boolean
    ) : ModEffect()

    data class Gravity(
        val direction: GravityDirection
    ) : ModEffect()

    data class Mutation(
        val type: MutationType,
        val affectedIndices: List<Int>
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
