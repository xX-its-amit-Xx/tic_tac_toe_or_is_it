package com.tictactoe.orisit.model

data class Cell(
    val row: Int,
    val col: Int
) {
    fun isValid(): Boolean = row in 0..2 && col in 0..2

    fun toIndex(): Int = row * 3 + col

    companion object {
        fun fromIndex(index: Int): Cell = Cell(index / 3, index % 3)

        val ALL_CELLS: List<Cell> = (0..8).map { fromIndex(it) }
    }
}
