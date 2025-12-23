package com.tictactoe.orisit.ui

import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tictactoe.orisit.R
import com.tictactoe.orisit.databinding.ActivityMainBinding
import com.tictactoe.orisit.model.AIDifficulty
import com.tictactoe.orisit.model.GameMode
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.ModPool
import com.tictactoe.orisit.mod.GravityMod
import com.tictactoe.orisit.mod.IMod
import com.tictactoe.orisit.mod.MutationMod
import com.tictactoe.orisit.mod.RotationMod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get configuration from intent
        val boardSize = intent.getIntExtra(MainMenuActivity.EXTRA_BOARD_SIZE, 3)
        val gameModeName = intent.getStringExtra(MainMenuActivity.EXTRA_GAME_MODE) ?: GameMode.VS_AI.name
        val aiDifficultyName = intent.getStringExtra(MainMenuActivity.EXTRA_AI_DIFFICULTY) ?: AIDifficulty.NORMAL.name
        val modPoolName = intent.getStringExtra(MainMenuActivity.EXTRA_MOD_POOL) ?: ModPool.NORMAL.name
        val modCount = intent.getIntExtra(MainMenuActivity.EXTRA_MOD_COUNT, 1)

        val gameMode = try { GameMode.valueOf(gameModeName) } catch (e: Exception) { GameMode.VS_AI }
        val aiDifficulty = try { AIDifficulty.valueOf(aiDifficultyName) } catch (e: Exception) { AIDifficulty.NORMAL }
        val modPool = try { ModPool.valueOf(modPoolName) } catch (e: Exception) { ModPool.NORMAL }

        // Initialize ViewModel with configuration
        viewModel.initialize(boardSize, gameMode, aiDifficulty, modPool, modCount)

        setupGameView()
        setupButtons()
        observeState()
        observeEvents()
    }

    private fun setupGameView() {
        binding.gameView.setOnCellClickListener { cell ->
            viewModel.onCellClicked(cell)
        }
    }

    private fun setupButtons() {
        binding.playAgainButton.setOnClickListener {
            hideResult()
            viewModel.startNewGame()
        }

        binding.modRevealOverlay.setOnClickListener {
            hideModReveal()
            viewModel.onModRevealDismissed()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateBoard(state)
                    updateTurnIndicator(state)
                    updateModCard(state)
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvents.collect { event ->
                    when (event) {
                        is UiEvent.ShowModReveal -> showModReveal(event.mod)
                        is UiEvent.AnimateModEffect -> animateModEffect(event.effect)
                        is UiEvent.ShowGameResult -> showResult(event.won)
                    }
                }
            }
        }
    }

    private fun updateBoard(state: GameUiState) {
        binding.gameView.setBoard(state.gameState.board)
        binding.gameView.setInputEnabled(state.inputEnabled)

        // Update gravity indicator
        val mod = state.matchConfig?.mod
        if (state.modActivated && mod is GravityMod) {
            binding.gameView.setGravityIndicator(mod.direction)
        } else {
            binding.gameView.setGravityIndicator(null)
        }

        // Update mutation preview highlights
        val pendingMutation = state.gameState.pendingMutation
        if (pendingMutation != null) {
            binding.gameView.setHighlightedCells(pendingMutation.affectedIndices.toSet())
        } else {
            binding.gameView.setHighlightedCells(emptySet())
        }
    }

    private fun updateTurnIndicator(state: GameUiState) {
        binding.turnIndicator.text = when {
            state.gameOver -> ""
            state.isPlayerTurn -> getString(R.string.your_turn)
            else -> getString(R.string.ai_turn)
        }

        binding.turnIndicator.setTextColor(
            getColor(
                if (state.isPlayerTurn) R.color.player_x else R.color.player_o
            )
        )

        binding.turnCounter.text = getString(R.string.turn_count, state.gameState.turnCount)
    }

    private fun updateModCard(state: GameUiState) {
        val mod = state.matchConfig?.mod
        
        if (state.modActivated && mod != null) {
            binding.modCard.isVisible = true
            binding.modTitle.text = "${mod.icon} ${mod.displayName}"
            binding.modDescription.text = mod.description

            val color = when (mod) {
                is RotationMod -> R.color.mod_rotation
                is GravityMod -> R.color.mod_gravity
                is MutationMod -> R.color.mod_mutation
                else -> R.color.accent
            }
            binding.modTitle.setTextColor(getColor(color))
        } else {
            binding.modCard.isVisible = false
        }
    }

    private fun showModReveal(mod: IMod) {
        binding.revealModName.text = "${mod.icon} ${mod.displayName}"
        binding.revealModDescription.text = mod.description

        binding.modRevealOverlay.apply {
            alpha = 0f
            isVisible = true
            animate()
                .alpha(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Animate mod name
        binding.revealModName.apply {
            scaleX = 0.5f
            scaleY = 0.5f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .setInterpolator(OvershootInterpolator())
                .start()
        }

        // Auto-dismiss after delay
        lifecycleScope.launch {
            delay(2500)
            if (binding.modRevealOverlay.isVisible) {
                hideModReveal()
                viewModel.onModRevealDismissed()
            }
        }
    }

    private fun hideModReveal() {
        binding.modRevealOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.modRevealOverlay.isVisible = false
            }
            .start()
    }

    private fun animateModEffect(effect: ModEffect) {
        when (effect) {
            is ModEffect.Rotation -> {
                binding.gameView.animateRotation(effect.angle, effect.clockwise) {
                    binding.gameView.setBoard(viewModel.uiState.value.gameState.board)
                }
            }
            is ModEffect.Gravity -> {
                binding.gameView.animateGravity(effect.direction) {
                    binding.gameView.setBoard(viewModel.uiState.value.gameState.board)
                }
            }
            is ModEffect.Mutation -> {
                binding.gameView.setHighlightedCells(effect.affectedIndices.toSet())
                binding.gameView.animateMutation {
                    binding.gameView.setBoard(viewModel.uiState.value.gameState.board)
                }
            }
            is ModEffect.Slide,
            is ModEffect.TileBreak,
            is ModEffect.TileFreeze,
            is ModEffect.TrapTriggered,
            is ModEffect.MoveDecay,
            is ModEffect.DoubleTurn,
            is ModEffect.FogUpdate,
            is ModEffect.DelayedReveal,
            is ModEffect.WinConditionChange,
            is ModEffect.AbilityUsed,
            is ModEffect.RandomEffect,
            is ModEffect.BoardResize,
            is ModEffect.TileDrift,
            is ModEffect.SubModActivated -> {
                // Update board immediately for these effects
                binding.gameView.setBoard(viewModel.uiState.value.gameState.board)
            }
        }
    }

    private fun showResult(won: Boolean?) {
        binding.resultText.text = when (won) {
            true -> getString(R.string.you_win)
            false -> getString(R.string.ai_wins)
            null -> getString(R.string.draw)
        }

        binding.resultText.setTextColor(
            getColor(
                when (won) {
                    true -> R.color.player_x
                    false -> R.color.player_o
                    null -> R.color.accent
                }
            )
        )

        binding.resultContainer.apply {
            alpha = 0f
            translationY = 50f
            isVisible = true
            
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setInterpolator(OvershootInterpolator())
                .start()
        }
    }

    private fun hideResult() {
        binding.resultContainer.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(200)
            .withEndAction {
                binding.resultContainer.isVisible = false
            }
            .start()
    }
}
