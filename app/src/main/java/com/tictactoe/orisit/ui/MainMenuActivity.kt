package com.tictactoe.orisit.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.tictactoe.orisit.R
import com.tictactoe.orisit.databinding.ActivityMainMenuBinding
import com.tictactoe.orisit.model.AIDifficulty
import com.tictactoe.orisit.model.GameMode
import com.tictactoe.orisit.model.ModPool

/**
 * Main menu activity for game mode selection.
 */
class MainMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMenuBinding
    
    private var selectedBoardSize = 4
    private var selectedAIDifficulty = AIDifficulty.NORMAL
    private var selectedModPool = ModPool.NORMAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBoardSizeSelector()
        setupDifficultySelector()
        setupModPoolSelector()
        setupButtons()
    }

    private fun setupBoardSizeSelector() {
        val sizes = arrayOf("3×3 Classic", "4×4 Extended")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sizes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.boardSizeSpinner.adapter = adapter
        binding.boardSizeSpinner.setSelection(1) // Default to 4×4
        
        binding.boardSizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBoardSize = if (position == 0) 3 else 4
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDifficultySelector() {
        val difficulties = arrayOf("Easy", "Normal")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.difficultySpinner.adapter = adapter
        binding.difficultySpinner.setSelection(1) // Default to Normal
        
        binding.difficultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedAIDifficulty = if (position == 0) AIDifficulty.EASY else AIDifficulty.NORMAL
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupModPoolSelector() {
        val pools = arrayOf("Normal", "Chaos")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pools)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.modPoolSpinner.adapter = adapter
        
        binding.modPoolSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedModPool = if (position == 0) ModPool.NORMAL else ModPool.CHAOS
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        binding.playVsAiButton.setOnClickListener {
            startGame(GameMode.VS_AI)
        }

        binding.playLocalButton.setOnClickListener {
            startGame(GameMode.LOCAL_MULTIPLAYER)
        }

        binding.playBluetoothButton.setOnClickListener {
            startBluetoothSetup()
        }

        binding.howToPlayButton.setOnClickListener {
            startActivity(Intent(this, HowToPlayActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            // TODO: Implement settings activity
        }
    }

    private fun startGame(mode: GameMode) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_GAME_MODE, mode.name)
            putExtra(EXTRA_BOARD_SIZE, selectedBoardSize)
            putExtra(EXTRA_AI_DIFFICULTY, selectedAIDifficulty.name)
            putExtra(EXTRA_MOD_POOL, selectedModPool.name)
        }
        startActivity(intent)
    }

    private fun startBluetoothSetup() {
        val intent = Intent(this, BluetoothSetupActivity::class.java).apply {
            putExtra(EXTRA_BOARD_SIZE, selectedBoardSize)
            putExtra(EXTRA_MOD_POOL, selectedModPool.name)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_GAME_MODE = "game_mode"
        const val EXTRA_BOARD_SIZE = "board_size"
        const val EXTRA_AI_DIFFICULTY = "ai_difficulty"
        const val EXTRA_MOD_POOL = "mod_pool"
    }
}
