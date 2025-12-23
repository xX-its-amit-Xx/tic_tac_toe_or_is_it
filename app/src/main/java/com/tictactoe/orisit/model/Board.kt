package com.tictactoe.orisit.model

data class Board(
    private val cells: Array<Player> = Array(9) { Player.NONE },
    private val disabledCells: MutableSet<Int> = mutableSetOf()
) {
    fun get(cell: Cell): Player = get(cell.row, cell.col)

    fun get(row: Int, col: Int): Player {
        if (row !in 0..2 || col !in 0..2) return Player.NONE
        return cells[row * 3 + col]
    }

    fun set(cell: Cell, player: Player): Board {
        return set(cell.row, cell.col, player)
    }

    fun set(row: Int, col: Int, player: Player): Board {
        if (row !in 0..2 || col !in 0..2) return this
        val newCells = cells.copyOf()
        newCells[row * 3 + col] = player
        return copy(cells = newCells)
    }

    fun isDisabled(cell: Cell): Boolean = disabledCells.contains(cell.toIndex())

    fun disableCell(cell: Cell): Board {
        val newDisabled = disabledCells.toMutableSet()
        newDisabled.add(cell.toIndex())
        return copy(disabledCells = newDisabled)
    }

    fun getDisabledCells(): Set<Int> = disabledCells.toSet()

    fun isEmpty(cell: Cell): Boolean = get(cell) == Player.NONE && !isDisabled(cell)

    fun isFull(): Boolean = cells.indices.all { 
        cells[it] != Player.NONE || disabledCells.contains(it) 
    }

    fun getEmptyCells(): List<Cell> {
        return Cell.ALL_CELLS.filter { isEmpty(it) }
    }

    fun checkWinner(): Player {
        val lines = listOf(
            listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)),
            listOf(Cell(1, 0), Cell(1, 1), Cell(1, 2)),
            listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2)),
            listOf(Cell(0, 0), Cell(1, 0), Cell(2, 0)),
            listOf(Cell(0, 1), Cell(1, 1), Cell(2, 1)),
            listOf(Cell(0, 2), Cell(1, 2), Cell(2, 2)),
            listOf(Cell(0, 0), Cell(1, 1), Cell(2, 2)),
            listOf(Cell(0, 2), Cell(1, 1), Cell(2, 0))
        )

        for (line in lines) {
            val first = get(line[0])
            if (first != Player.NONE && 
                line.all { get(it) == first && !isDisabled(it) }) {
                return first
            }
        }
        return Player.NONE
    }

    fun rotate90Clockwise(): Board {
        val newCells = Array(9) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        
        for (row in 0..2) {
            for (col in 0..2) {
                val newRow = col
                val newCol = 2 - row
                newCells[newRow * 3 + newCol] = cells[row * 3 + col]
                if (disabledCells.contains(row * 3 + col)) {
                    newDisabled.add(newRow * 3 + newCol)
                }
            }
        }
        return Board(newCells, newDisabled)
    }

    fun rotate90CounterClockwise(): Board {
        val newCells = Array(9) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        
        for (row in 0..2) {
            for (col in 0..2) {
                val newRow = 2 - col
                val newCol = row
                newCells[newRow * 3 + newCol] = cells[row * 3 + col]
                if (disabledCells.contains(row * 3 + col)) {
                    newDisabled.add(newRow * 3 + newCol)
                }
            }
        }
        return Board(newCells, newDisabled)
    }

    fun rotate180(): Board {
        val newCells = Array(9) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        
        for (i in 0..8) {
            newCells[8 - i] = cells[i]
            if (disabledCells.contains(i)) {
                newDisabled.add(8 - i)
            }
        }
        return Board(newCells, newDisabled)
    }

    fun swapRows(row1: Int, row2: Int): Board {
        if (row1 !in 0..2 || row2 !in 0..2) return this
        val newCells = cells.copyOf()
        val newDisabled = mutableSetOf<Int>()
        
        for (col in 0..2) {
            val idx1 = row1 * 3 + col
            val idx2 = row2 * 3 + col
            newCells[idx1] = cells[idx2]
            newCells[idx2] = cells[idx1]
            
            if (disabledCells.contains(idx1)) newDisabled.add(idx2)
            else if (disabledCells.contains(idx2)) newDisabled.add(idx1)
            
            disabledCells.forEach { 
                if (it != idx1 && it != idx2) newDisabled.add(it) 
            }
        }
        return Board(newCells, newDisabled)
    }

    fun swapColumns(col1: Int, col2: Int): Board {
        if (col1 !in 0..2 || col2 !in 0..2) return this
        val newCells = cells.copyOf()
        val newDisabled = mutableSetOf<Int>()
        
        for (row in 0..2) {
            val idx1 = row * 3 + col1
            val idx2 = row * 3 + col2
            newCells[idx1] = cells[idx2]
            newCells[idx2] = cells[idx1]
        }
        
        disabledCells.forEach { idx ->
            val r = idx / 3
            val c = idx % 3
            when (c) {
                col1 -> newDisabled.add(r * 3 + col2)
                col2 -> newDisabled.add(r * 3 + col1)
                else -> newDisabled.add(idx)
            }
        }
        return Board(newCells, newDisabled)
    }

    fun applyGravity(direction: GravityDirection): Board {
        val newCells = Array(9) { Player.NONE }
        val newDisabled = disabledCells.toMutableSet()

        when (direction) {
            GravityDirection.DOWN -> {
                for (col in 0..2) {
                    var writeRow = 2
                    for (row in 2 downTo 0) {
                        val idx = row * 3 + col
                        if (disabledCells.contains(idx)) {
                            writeRow = row - 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeRow >= 0 && disabledCells.contains(writeRow * 3 + col)) {
                                writeRow--
                            }
                            if (writeRow >= 0) {
                                newCells[writeRow * 3 + col] = cells[idx]
                                writeRow--
                            }
                        }
                    }
                }
            }
            GravityDirection.UP -> {
                for (col in 0..2) {
                    var writeRow = 0
                    for (row in 0..2) {
                        val idx = row * 3 + col
                        if (disabledCells.contains(idx)) {
                            writeRow = row + 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeRow <= 2 && disabledCells.contains(writeRow * 3 + col)) {
                                writeRow++
                            }
                            if (writeRow <= 2) {
                                newCells[writeRow * 3 + col] = cells[idx]
                                writeRow++
                            }
                        }
                    }
                }
            }
            GravityDirection.LEFT -> {
                for (row in 0..2) {
                    var writeCol = 0
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        if (disabledCells.contains(idx)) {
                            writeCol = col + 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeCol <= 2 && disabledCells.contains(row * 3 + writeCol)) {
                                writeCol++
                            }
                            if (writeCol <= 2) {
                                newCells[row * 3 + writeCol] = cells[idx]
                                writeCol++
                            }
                        }
                    }
                }
            }
            GravityDirection.RIGHT -> {
                for (row in 0..2) {
                    var writeCol = 2
                    for (col in 2 downTo 0) {
                        val idx = row * 3 + col
                        if (disabledCells.contains(idx)) {
                            writeCol = col - 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeCol >= 0 && disabledCells.contains(row * 3 + writeCol)) {
                                writeCol--
                            }
                            if (writeCol >= 0) {
                                newCells[row * 3 + writeCol] = cells[idx]
                                writeCol--
                            }
                        }
                    }
                }
            }
        }
        return Board(newCells, newDisabled)
    }

    fun copy(): Board = Board(cells.copyOf(), disabledCells.toMutableSet())

    fun toArray(): Array<Player> = cells.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        return cells.contentEquals(other.cells) && disabledCells == other.disabledCells
    }

    override fun hashCode(): Int {
        return 31 * cells.contentHashCode() + disabledCells.hashCode()
    }
}

enum class GravityDirection {
    DOWN, UP, LEFT, RIGHT;

    fun symbol(): String = when (this) {
        DOWN -> "⬇"
        UP -> "⬆"
        LEFT -> "⬅"
        RIGHT -> "➡"
    }
}
