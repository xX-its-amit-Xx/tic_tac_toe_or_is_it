package com.tictactoe.orisit.model

enum class Player {
    X,
    O,
    NONE;

    fun opponent(): Player = when (this) {
        X -> O
        O -> X
        NONE -> NONE
    }

    fun symbol(): String = when (this) {
        X -> "X"
        O -> "O"
        NONE -> ""
    }
}
