package com.tictactoe.orisit.model

data class Cell(
    val row: Int,
    val col: Int
) {
    fun isValid(size: Int = 3): Boolean = row in 0 until size && col in 0 until size

    fun toIndex(size: Int = 3): Int = row * size + col

    companion object {
        fun fromIndex(index: Int, size: Int = 3): Cell = Cell(index / size, index % size)

        fun allCells(size: Int = 3): List<Cell> = (0 until size * size).map { fromIndex(it, size) }

        val ALL_CELLS_3X3: List<Cell> = allCells(3)
        val ALL_CELLS_4X4: List<Cell> = allCells(4)
        val ALL_CELLS_5X5: List<Cell> = allCells(5)
    }
}
