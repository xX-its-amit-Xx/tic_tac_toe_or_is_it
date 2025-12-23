package com.tictactoe.orisit.engine

import com.tictactoe.orisit.ai.GameAI
import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameMode
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.GameStatus
import com.tictactoe.orisit.model.MatchConfig
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core game engine managing state transitions.
 * Handles input, simulation, and mod effects.
 * Supports configurable board sizes and multiple game modes.
 */
class GameEngine {

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _events = MutableStateFlow<GameEvent?>(null)
    val events: StateFlow<GameEvent?> = _events.asStateFlow()

    private var matchConfig: MatchConfig? = null
    private var ai: GameAI? = null

    /**
     * Start a new match with the given configuration.
     */
    fun startNewMatch(config: MatchConfig = MatchConfig.generate()) {
        matchConfig = config
        
        // Create AI if needed
        ai = if (config.gameMode == GameMode.VS_AI) {
            GameAI(config.humanPlayer.opponent(), config.aiDifficulty)
        } else {
            null
        }
        
        // Create board with configured size
        val initialBoard = config.createInitialBoard()
        _gameState.value = GameState(board = initialBoard)
        _events.value = GameEvent.MatchStarted(config)
    }

    /**
     * Process a player's move at the given cell.
     * Returns true if the move was valid.
     */
    fun makeMove(cell: Cell): Boolean {
        val state = _gameState.value
        val config = matchConfig ?: return false

        // Validate move
        if (state.isGameOver()) return false
        if (!state.board.isEmpty(cell)) return false
        if (state.currentPlayer != config.humanPlayer) return false

        // Apply move
        val newBoard = state.board.set(cell, state.currentPlayer)
        var newState = state.copy(
            board = newBoard,
            turnCount = state.turnCount + 1
        )

        // Check for win/draw
        newState = checkGameEnd(newState)
        if (newState.isGameOver()) {
            _gameState.value = newState
            emitGameEndEvent(newState)
            return true
        }

        // Check mod activation
        newState = checkModActivation(newState, config)

        // Apply mod effects
        val (stateAfterMod, effect) = applyModEffects(newState, config)
        newState = stateAfterMod

        if (effect != null) {
            _events.value = GameEvent.ModEffectApplied(effect)
        }

        // Check for win after mod effects
        newState = checkGameEnd(newState)
        if (newState.isGameOver()) {
            _gameState.value = newState
            emitGameEndEvent(newState)
            return true
        }

        // Switch to next player
        newState = newState.switchPlayer()
        _gameState.value = newState

        return true
    }

    /**
     * Execute AI turn.
     */
    fun executeAITurn(): Cell? {
        val state = _gameState.value
        val config = matchConfig ?: return null
        val currentAI = ai ?: return null

        if (state.isGameOver()) return null
        if (state.currentPlayer == config.humanPlayer) return null

        val move = currentAI.selectMove(state) ?: return null

        // Apply AI move
        val newBoard = state.board.set(move, state.currentPlayer)
        var newState = state.copy(
            board = newBoard,
            turnCount = state.turnCount + 1
        )

        // Check for win/draw
        newState = checkGameEnd(newState)
        if (newState.isGameOver()) {
            _gameState.value = newState
            emitGameEndEvent(newState)
            return move
        }

        // Check mod activation
        newState = checkModActivation(newState, config)

        // Apply mod effects
        val (stateAfterMod, effect) = applyModEffects(newState, config)
        newState = stateAfterMod

        if (effect != null) {
            _events.value = GameEvent.ModEffectApplied(effect)
        }

        // Check for win after mod effects
        newState = checkGameEnd(newState)
        if (newState.isGameOver()) {
            _gameState.value = newState
            emitGameEndEvent(newState)
            return move
        }

        // Switch player
        newState = newState.switchPlayer()
        _gameState.value = newState

        return move
    }

    private fun checkModActivation(state: GameState, config: MatchConfig): GameState {
        if (!state.modActivated && state.turnCount >= config.modActivationTurn) {
            _events.value = GameEvent.ModActivated(config.mod)
            return config.mod.onActivate(state)
        }
        return state
    }

    private fun applyModEffects(state: GameState, config: MatchConfig): Pair<GameState, ModEffect?> {
        if (!state.modActivated) return Pair(state, null)
        return config.mod.onTurnEnd(state)
    }

    private fun checkGameEnd(state: GameState): GameState {
        // Check with effective win condition (may be modified by mods)
        val winCondition = state.getEffectiveWinCondition()
        var winner = state.board.checkWinner(winCondition)
        
        // Also check for square wins if applicable
        if (winner == Player.NONE && state.shouldCheckSquareWin()) {
            winner = state.board.checkSquareWin()
        }
        
        return when {
            winner != Player.NONE -> state.setWinner(winner)
            state.board.isFull() -> state.copy(status = GameStatus.DRAW)
            else -> state
        }
    }

    private fun emitGameEndEvent(state: GameState) {
        _events.value = when (state.status) {
            GameStatus.WON -> GameEvent.GameWon(state.winner)
            GameStatus.DRAW -> GameEvent.GameDraw
            else -> null
        }
    }

    /**
     * Check if it's currently the human player's turn.
     */
    fun isHumanTurn(): Boolean {
        val config = matchConfig ?: return false
        return _gameState.value.currentPlayer == config.humanPlayer && 
               !_gameState.value.isGameOver()
    }

    /**
     * Check if it's currently the AI's turn.
     */
    fun isAITurn(): Boolean {
        val config = matchConfig ?: return false
        return _gameState.value.currentPlayer != config.humanPlayer && 
               !_gameState.value.isGameOver()
    }

    /**
     * Get the current match configuration.
     */
    fun getMatchConfig(): MatchConfig? = matchConfig

    /**
     * Clear the current event after handling.
     */
    fun clearEvent() {
        _events.value = null
    }
}

/**
 * Events emitted by the game engine for UI updates.
 */
sealed class GameEvent {
    data class MatchStarted(val config: MatchConfig) : GameEvent()
    data class ModActivated(val mod: com.tictactoe.orisit.mod.IMod) : GameEvent()
    data class ModEffectApplied(val effect: ModEffect) : GameEvent()
    data class GameWon(val winner: Player) : GameEvent()
    data object GameDraw : GameEvent()
}
