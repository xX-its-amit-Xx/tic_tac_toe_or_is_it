package com.tictactoe.orisit.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tictactoe.orisit.databinding.ActivityHowToPlayBinding
import com.tictactoe.orisit.mod.ModCategory
import com.tictactoe.orisit.mod.ModFactory

/**
 * Activity showing how to play the game and explaining mod categories.
 */
class HowToPlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHowToPlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHowToPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupContent()
        
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupContent() {
        val content = buildString {
            appendLine("# Tic Tac Toe... Or Is It?")
            appendLine()
            appendLine("A Tic-Tac-Toe variant where mid-game rule modifications (\"Mods\") activate to twist the classic gameplay.")
            appendLine()
            appendLine("## Basic Rules")
            appendLine("• Place your mark (X or O) on empty cells")
            appendLine("• First to get required marks in a row wins")
            appendLine("• 3×3 board: 3 in a row to win")
            appendLine("• 4×4 board: 4 in a row to win")
            appendLine()
            appendLine("## The Mod System")
            appendLine("• First 5 turns play normally")
            appendLine("• On turn 5-6, a hidden Mod activates")
            appendLine("• Mod parameters are revealed when active")
            appendLine("• Adapt your strategy to win!")
            appendLine()
            appendLine("## Mod Categories")
            appendLine()
            
            for (category in ModCategory.entries) {
                appendLine("### ${category.icon} ${category.name.replace('_', ' ')}")
                val mods = ModFactory.getModsByCategory(category)
                for (modType in mods) {
                    appendLine("• ${modType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}")
                }
                appendLine()
            }
            
            appendLine("## Tips")
            appendLine("• Watch for visual indicators of upcoming effects")
            appendLine("• Adapt quickly when mods activate")
            appendLine("• Center and corner control remains important")
            appendLine("• On 4×4, diagonal wins are still possible!")
        }
        
        binding.contentText.text = content
    }
}
