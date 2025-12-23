package com.tictactoe.orisit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tictactoe.orisit.engine.GameEngine
import com.tictactoe.orisit.engine.GameEvent
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.MatchConfig
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.mod.IMod
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel managing game state and UI events.
 */
class GameViewModel : ViewModel() {

    private val engine = GameEngine()

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    init {
        observeGameState()
        observeGameEvents()
        startNewGame()
    }

    private fun observeGameState() {
        viewModelScope.launch {
            engine.gameState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    gameState = state,
                    isPlayerTurn = engine.isHumanTurn(),
                    inputEnabled = engine.isHumanTurn() && !state.isGameOver()
                )
            }
        }
    }

    private fun observeGameEvents() {
        viewModelScope.launch {
            engine.events.collect { event ->
                when (event) {
                    is GameEvent.MatchStarted -> {
                        _uiState.value = _uiState.value.copy(
                            matchConfig = event.config,
                            modActivated = false,
                            gameOver = false
                        )
                    }
                    is GameEvent.ModActivated -> {
                        _uiState.value = _uiState.value.copy(modActivated = true)
                        _uiEvents.emit(UiEvent.ShowModReveal(event.mod))
                    }
                    is GameEvent.ModEffectApplied -> {
                        _uiEvents.emit(UiEvent.AnimateModEffect(event.effect))
                    }
                    is GameEvent.GameWon -> {
                        _uiState.value = _uiState.value.copy(gameOver = true)
                        _uiEvents.emit(UiEvent.ShowGameResult(
                            won = event.winner == engine.getMatchConfig()?.humanPlayer
                        ))
                    }
                    is GameEvent.GameDraw -> {
                        _uiState.value = _uiState.value.copy(gameOver = true)
                        _uiEvents.emit(UiEvent.ShowGameResult(won = null))
                    }
                    null -> {}
                }
                engine.clearEvent()
            }
        }
    }

    fun startNewGame() {
        engine.startNewMatch(MatchConfig.generate())
    }

    fun onCellClicked(cell: Cell) {
        if (!engine.isHumanTurn()) return

        viewModelScope.launch {
            val moved = engine.makeMove(cell)
            if (moved && engine.isAITurn()) {
                scheduleAITurn()
            }
        }
    }

    private fun scheduleAITurn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(inputEnabled = false)
            delay(AI_THINK_DELAY)
            
            engine.executeAITurn()
            
            if (engine.isHumanTurn()) {
                _uiState.value = _uiState.value.copy(inputEnabled = true)
            } else if (engine.isAITurn()) {
                scheduleAITurn()
            }
        }
    }

    fun onModRevealDismissed() {
        viewModelScope.launch {
            if (engine.isAITurn()) {
                scheduleAITurn()
            }
        }
    }

    companion object {
        private const val AI_THINK_DELAY = 600L
    }
}

/**
 * UI state for the game screen.
 */
data class GameUiState(
    val gameState: GameState = GameState(),
    val matchConfig: MatchConfig? = null,
    val isPlayerTurn: Boolean = true,
    val inputEnabled: Boolean = true,
    val modActivated: Boolean = false,
    val gameOver: Boolean = false
)

/**
 * One-shot UI events.
 */
sealed class UiEvent {
    data class ShowModReveal(val mod: IMod) : UiEvent()
    data class AnimateModEffect(val effect: ModEffect) : UiEvent()
    data class ShowGameResult(val won: Boolean?) : UiEvent()
}
