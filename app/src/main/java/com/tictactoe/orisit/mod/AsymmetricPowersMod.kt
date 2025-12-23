package com.tictactoe.orisit.mod

import com.tictactoe.orisit.model.AbilityType
import com.tictactoe.orisit.model.GameState
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.Player
import com.tictactoe.orisit.model.PlayerAbility
import kotlin.random.Random

/**
 * RULE MUTATION MOD: ASYMMETRIC POWERS
 * Each player gets a unique ability.
 */
class AsymmetricPowersMod(
    val xAbility: AbilityType,
    val oAbility: AbilityType,
    val uses: Int                  // Uses per player (1-3)
) : BaseMod() {

    override val modId = "asymmetric_powers"
    override val displayName = "Asymmetric Powers"
    override val icon = "⚔️"
    override val category = ModCategory.RULE_MUTATION
    override val description: String
        get() {
            val xDesc = abilityDescription(xAbility)
            val oDesc = abilityDescription(oAbility)
            return "X: $xDesc | O: $oDesc ($uses uses each)"
        }

    private fun abilityDescription(ability: AbilityType): String = when (ability) {
        AbilityType.EXTRA_MOVE -> "Extra move"
        AbilityType.REMOVE_OPPONENT_MARK -> "Remove mark"
        AbilityType.SWAP_ANY_MARKS -> "Swap marks"
        AbilityType.FREEZE_CELL -> "Freeze cell"
        AbilityType.REVEAL_HIDDEN -> "Reveal hidden"
        AbilityType.BLOCK_MOD_EFFECT -> "Block effect"
    }

    override fun onActivate(state: GameState): GameState {
        var newState = super.onActivate(state)
        
        newState = newState.setPlayerAbility(
            Player.X,
            PlayerAbility(xAbility, uses, uses)
        )
        newState = newState.setPlayerAbility(
            Player.O,
            PlayerAbility(oAbility, uses, uses)
        )
        
        return newState
    }

    override fun shouldTrigger(state: GameState): Boolean = false

    override fun onTurnEnd(state: GameState): Pair<GameState, ModEffect?> {
        return Pair(state, null)
    }

    override fun previewNextEffect(state: GameState): ModEffect? = null

    override fun getParameterSummary(): String {
        return "X: ${xAbility.name}, O: ${oAbility.name}, $uses uses"
    }

    companion object {
        fun randomize(random: Random = Random, boardSize: Int = 3): AsymmetricPowersMod {
            val abilities = AbilityType.entries.shuffled(random)
            
            return AsymmetricPowersMod(
                xAbility = abilities[0],
                oAbility = abilities[1],
                uses = random.nextInt(1, 4)
            )
        }
    }
}
