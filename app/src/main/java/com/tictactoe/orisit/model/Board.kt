package com.tictactoe.orisit.model

data class Board(
    val size: Int = 3,
    val winCondition: Int = size,
    private val cells: Array<Player> = Array(size * size) { Player.NONE },
    private val disabledCells: MutableSet<Int> = mutableSetOf(),
    private val cellDurability: MutableMap<Int, Int> = mutableMapOf(),
    private val frozenCells: MutableSet<Int> = mutableSetOf(),
    private val trapCells: MutableMap<Int, TrapType> = mutableMapOf(),
    private val cellPlacementTurn: MutableMap<Int, Int> = mutableMapOf(),
    private val delayedPlacements: MutableList<DelayedPlacement> = mutableListOf(),
    private val hiddenCells: MutableSet<Int> = mutableSetOf()
) {
    fun get(cell: Cell): Player = get(cell.row, cell.col)

    fun get(row: Int, col: Int): Player {
        if (row !in 0 until size || col !in 0 until size) return Player.NONE
        return cells[row * size + col]
    }

    fun set(cell: Cell, player: Player, turnNumber: Int = -1): Board {
        return set(cell.row, cell.col, player, turnNumber)
    }

    fun set(row: Int, col: Int, player: Player, turnNumber: Int = -1): Board {
        if (row !in 0 until size || col !in 0 until size) return this
        val idx = row * size + col
        val newCells = cells.copyOf()
        newCells[idx] = player
        val newPlacementTurn = cellPlacementTurn.toMutableMap()
        if (turnNumber >= 0 && player != Player.NONE) {
            newPlacementTurn[idx] = turnNumber
        }
        return copy(cells = newCells, cellPlacementTurn = newPlacementTurn)
    }

    fun isDisabled(cell: Cell): Boolean = disabledCells.contains(cell.toIndex(size))

    fun disableCell(cell: Cell): Board {
        val newDisabled = disabledCells.toMutableSet()
        newDisabled.add(cell.toIndex(size))
        return copy(disabledCells = newDisabled)
    }

    fun getDisabledCells(): Set<Int> = disabledCells.toSet()

    fun isEmpty(cell: Cell): Boolean = get(cell) == Player.NONE && !isDisabled(cell) && !isFrozen(cell)

    fun isFull(): Boolean = cells.indices.all { 
        cells[it] != Player.NONE || disabledCells.contains(it) 
    }

    fun getEmptyCells(): List<Cell> {
        return Cell.allCells(size).filter { isEmpty(it) }
    }

    fun isFrozen(cell: Cell): Boolean = frozenCells.contains(cell.toIndex(size))

    fun freezeCell(cell: Cell): Board {
        val newFrozen = frozenCells.toMutableSet()
        newFrozen.add(cell.toIndex(size))
        return copy(frozenCells = newFrozen)
    }

    fun unfreezeCell(cell: Cell): Board {
        val newFrozen = frozenCells.toMutableSet()
        newFrozen.remove(cell.toIndex(size))
        return copy(frozenCells = newFrozen)
    }

    fun getFrozenCells(): Set<Int> = frozenCells.toSet()

    fun setTrap(cell: Cell, trapType: TrapType): Board {
        val newTraps = trapCells.toMutableMap()
        newTraps[cell.toIndex(size)] = trapType
        return copy(trapCells = newTraps)
    }

    fun getTrap(cell: Cell): TrapType? = trapCells[cell.toIndex(size)]

    fun removeTrap(cell: Cell): Board {
        val newTraps = trapCells.toMutableMap()
        newTraps.remove(cell.toIndex(size))
        return copy(trapCells = newTraps)
    }

    fun getTrapCells(): Map<Int, TrapType> = trapCells.toMap()

    fun getDurability(cell: Cell): Int = cellDurability[cell.toIndex(size)] ?: -1

    fun setDurability(cell: Cell, durability: Int): Board {
        val newDurability = cellDurability.toMutableMap()
        newDurability[cell.toIndex(size)] = durability
        return copy(cellDurability = newDurability)
    }

    fun decrementDurability(cell: Cell): Board {
        val idx = cell.toIndex(size)
        val current = cellDurability[idx] ?: return this
        val newDurability = cellDurability.toMutableMap()
        newDurability[idx] = current - 1
        return copy(cellDurability = newDurability)
    }

    fun getPlacementTurn(cell: Cell): Int = cellPlacementTurn[cell.toIndex(size)] ?: -1

    fun addDelayedPlacement(placement: DelayedPlacement): Board {
        val newDelayed = delayedPlacements.toMutableList()
        newDelayed.add(placement)
        return copy(delayedPlacements = newDelayed)
    }

    fun getDelayedPlacements(): List<DelayedPlacement> = delayedPlacements.toList()

    fun processDelayedPlacements(currentTurn: Int): Board {
        val toApply = delayedPlacements.filter { it.revealTurn <= currentTurn }
        val remaining = delayedPlacements.filter { it.revealTurn > currentTurn }
        
        var newBoard = copy(delayedPlacements = remaining.toMutableList())
        for (placement in toApply) {
            if (newBoard.isEmpty(placement.cell)) {
                newBoard = newBoard.set(placement.cell, placement.player, placement.placedTurn)
            }
        }
        return newBoard
    }

    fun hideCell(cell: Cell): Board {
        val newHidden = hiddenCells.toMutableSet()
        newHidden.add(cell.toIndex(size))
        return copy(hiddenCells = newHidden)
    }

    fun revealCell(cell: Cell): Board {
        val newHidden = hiddenCells.toMutableSet()
        newHidden.remove(cell.toIndex(size))
        return copy(hiddenCells = newHidden)
    }

    fun isHidden(cell: Cell): Boolean = hiddenCells.contains(cell.toIndex(size))

    fun getHiddenCells(): Set<Int> = hiddenCells.toSet()

    fun checkWinner(): Player = checkWinner(winCondition)

    fun checkWinner(requiredInRow: Int): Player {
        val lines = generateWinningLines(requiredInRow)

        for (line in lines) {
            val first = get(line[0])
            if (first != Player.NONE && 
                line.all { get(it) == first && !isDisabled(it) }) {
                return first
            }
        }
        return Player.NONE
    }

    fun checkSquareWin(): Player {
        for (row in 0 until size - 1) {
            for (col in 0 until size - 1) {
                val cells = listOf(
                    Cell(row, col), Cell(row, col + 1),
                    Cell(row + 1, col), Cell(row + 1, col + 1)
                )
                val first = get(cells[0])
                if (first != Player.NONE && cells.all { get(it) == first && !isDisabled(it) }) {
                    return first
                }
            }
        }
        return Player.NONE
    }

    fun generateWinningLines(requiredInRow: Int = winCondition): List<List<Cell>> {
        val lines = mutableListOf<List<Cell>>()
        
        for (row in 0 until size) {
            for (startCol in 0..size - requiredInRow) {
                lines.add((0 until requiredInRow).map { Cell(row, startCol + it) })
            }
        }
        
        for (col in 0 until size) {
            for (startRow in 0..size - requiredInRow) {
                lines.add((0 until requiredInRow).map { Cell(startRow + it, col) })
            }
        }
        
        for (startRow in 0..size - requiredInRow) {
            for (startCol in 0..size - requiredInRow) {
                lines.add((0 until requiredInRow).map { Cell(startRow + it, startCol + it) })
            }
        }
        
        for (startRow in 0..size - requiredInRow) {
            for (startCol in requiredInRow - 1 until size) {
                lines.add((0 until requiredInRow).map { Cell(startRow + it, startCol - it) })
            }
        }
        
        return lines
    }

    fun rotate90Clockwise(): Board {
        val newCells = Array(size * size) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        val newFrozen = mutableSetOf<Int>()
        val newTraps = mutableMapOf<Int, TrapType>()
        val newDurability = mutableMapOf<Int, Int>()
        val newPlacementTurn = mutableMapOf<Int, Int>()
        val newHidden = mutableSetOf<Int>()
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val newRow = col
                val newCol = size - 1 - row
                val oldIdx = row * size + col
                val newIdx = newRow * size + newCol
                newCells[newIdx] = cells[oldIdx]
                if (disabledCells.contains(oldIdx)) newDisabled.add(newIdx)
                if (frozenCells.contains(oldIdx)) newFrozen.add(newIdx)
                if (hiddenCells.contains(oldIdx)) newHidden.add(newIdx)
                trapCells[oldIdx]?.let { newTraps[newIdx] = it }
                cellDurability[oldIdx]?.let { newDurability[newIdx] = it }
                cellPlacementTurn[oldIdx]?.let { newPlacementTurn[newIdx] = it }
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled, frozenCells = newFrozen, 
            trapCells = newTraps, cellDurability = newDurability, cellPlacementTurn = newPlacementTurn,
            hiddenCells = newHidden)
    }

    fun rotate90CounterClockwise(): Board {
        val newCells = Array(size * size) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        val newFrozen = mutableSetOf<Int>()
        val newTraps = mutableMapOf<Int, TrapType>()
        val newDurability = mutableMapOf<Int, Int>()
        val newPlacementTurn = mutableMapOf<Int, Int>()
        val newHidden = mutableSetOf<Int>()
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val newRow = size - 1 - col
                val newCol = row
                val oldIdx = row * size + col
                val newIdx = newRow * size + newCol
                newCells[newIdx] = cells[oldIdx]
                if (disabledCells.contains(oldIdx)) newDisabled.add(newIdx)
                if (frozenCells.contains(oldIdx)) newFrozen.add(newIdx)
                if (hiddenCells.contains(oldIdx)) newHidden.add(newIdx)
                trapCells[oldIdx]?.let { newTraps[newIdx] = it }
                cellDurability[oldIdx]?.let { newDurability[newIdx] = it }
                cellPlacementTurn[oldIdx]?.let { newPlacementTurn[newIdx] = it }
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled, frozenCells = newFrozen,
            trapCells = newTraps, cellDurability = newDurability, cellPlacementTurn = newPlacementTurn,
            hiddenCells = newHidden)
    }

    fun rotate180(): Board {
        val totalCells = size * size
        val newCells = Array(totalCells) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        val newFrozen = mutableSetOf<Int>()
        val newTraps = mutableMapOf<Int, TrapType>()
        val newDurability = mutableMapOf<Int, Int>()
        val newPlacementTurn = mutableMapOf<Int, Int>()
        val newHidden = mutableSetOf<Int>()
        
        for (i in 0 until totalCells) {
            val newIdx = totalCells - 1 - i
            newCells[newIdx] = cells[i]
            if (disabledCells.contains(i)) newDisabled.add(newIdx)
            if (frozenCells.contains(i)) newFrozen.add(newIdx)
            if (hiddenCells.contains(i)) newHidden.add(newIdx)
            trapCells[i]?.let { newTraps[newIdx] = it }
            cellDurability[i]?.let { newDurability[newIdx] = it }
            cellPlacementTurn[i]?.let { newPlacementTurn[newIdx] = it }
        }
        return copy(cells = newCells, disabledCells = newDisabled, frozenCells = newFrozen,
            trapCells = newTraps, cellDurability = newDurability, cellPlacementTurn = newPlacementTurn,
            hiddenCells = newHidden)
    }

    fun rotateRegion(region: RotationRegion, clockwise: Boolean): Board {
        val (startRow, startCol, regionSize) = when (region) {
            RotationRegion.FULL -> Triple(0, 0, size)
            RotationRegion.TOP_LEFT -> Triple(0, 0, (size + 1) / 2)
            RotationRegion.TOP_RIGHT -> Triple(0, size - (size + 1) / 2, (size + 1) / 2)
            RotationRegion.BOTTOM_LEFT -> Triple(size - (size + 1) / 2, 0, (size + 1) / 2)
            RotationRegion.BOTTOM_RIGHT -> Triple(size - (size + 1) / 2, size - (size + 1) / 2, (size + 1) / 2)
        }
        
        if (region == RotationRegion.FULL) {
            return if (clockwise) rotate90Clockwise() else rotate90CounterClockwise()
        }
        
        val newCells = cells.copyOf()
        val newDisabled = disabledCells.toMutableSet()
        val newFrozen = frozenCells.toMutableSet()
        
        for (r in 0 until regionSize) {
            for (c in 0 until regionSize) {
                val oldRow = startRow + r
                val oldCol = startCol + c
                val (newR, newC) = if (clockwise) {
                    Pair(c, regionSize - 1 - r)
                } else {
                    Pair(regionSize - 1 - c, r)
                }
                val newRow = startRow + newR
                val newCol = startCol + newC
                
                val oldIdx = oldRow * size + oldCol
                val newIdx = newRow * size + newCol
                newCells[newIdx] = cells[oldIdx]
                
                newDisabled.remove(newIdx)
                if (disabledCells.contains(oldIdx)) newDisabled.add(newIdx)
                newFrozen.remove(newIdx)
                if (frozenCells.contains(oldIdx)) newFrozen.add(newIdx)
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled, frozenCells = newFrozen)
    }

    fun slideRow(rowIndex: Int, direction: SlideDirection, amount: Int = 1): Board {
        if (rowIndex !in 0 until size) return this
        val newCells = cells.copyOf()
        
        for (col in 0 until size) {
            val newCol = when (direction) {
                SlideDirection.LEFT -> (col - amount + size) % size
                SlideDirection.RIGHT -> (col + amount) % size
                else -> col
            }
            newCells[rowIndex * size + newCol] = cells[rowIndex * size + col]
        }
        return copy(cells = newCells)
    }

    fun slideColumn(colIndex: Int, direction: SlideDirection, amount: Int = 1): Board {
        if (colIndex !in 0 until size) return this
        val newCells = cells.copyOf()
        
        for (row in 0 until size) {
            val newRow = when (direction) {
                SlideDirection.UP -> (row - amount + size) % size
                SlideDirection.DOWN -> (row + amount) % size
                else -> row
            }
            newCells[newRow * size + colIndex] = cells[row * size + colIndex]
        }
        return copy(cells = newCells)
    }

    fun swapRows(row1: Int, row2: Int): Board {
        if (row1 !in 0 until size || row2 !in 0 until size) return this
        val newCells = cells.copyOf()
        val newDisabled = mutableSetOf<Int>()
        
        for (col in 0 until size) {
            val idx1 = row1 * size + col
            val idx2 = row2 * size + col
            newCells[idx1] = cells[idx2]
            newCells[idx2] = cells[idx1]
        }
        
        disabledCells.forEach { idx ->
            val r = idx / size
            val c = idx % size
            when (r) {
                row1 -> newDisabled.add(row2 * size + c)
                row2 -> newDisabled.add(row1 * size + c)
                else -> newDisabled.add(idx)
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled)
    }

    fun swapColumns(col1: Int, col2: Int): Board {
        if (col1 !in 0 until size || col2 !in 0 until size) return this
        val newCells = cells.copyOf()
        val newDisabled = mutableSetOf<Int>()
        
        for (row in 0 until size) {
            val idx1 = row * size + col1
            val idx2 = row * size + col2
            newCells[idx1] = cells[idx2]
            newCells[idx2] = cells[idx1]
        }
        
        disabledCells.forEach { idx ->
            val r = idx / size
            val c = idx % size
            when (c) {
                col1 -> newDisabled.add(r * size + col2)
                col2 -> newDisabled.add(r * size + col1)
                else -> newDisabled.add(idx)
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled)
    }

    fun swapMarks(idx1: Int, idx2: Int): Board {
        if (idx1 !in 0 until size * size || idx2 !in 0 until size * size) return this
        val newCells = cells.copyOf()
        newCells[idx1] = cells[idx2]
        newCells[idx2] = cells[idx1]
        return copy(cells = newCells)
    }

    fun removeLastMark(): Board {
        val lastPlacement = cellPlacementTurn.maxByOrNull { it.value } ?: return this
        val newCells = cells.copyOf()
        newCells[lastPlacement.key] = Player.NONE
        val newPlacementTurn = cellPlacementTurn.toMutableMap()
        newPlacementTurn.remove(lastPlacement.key)
        return copy(cells = newCells, cellPlacementTurn = newPlacementTurn)
    }

    fun applyGravity(direction: GravityDirection): Board {
        val newCells = Array(size * size) { Player.NONE }
        val newDisabled = disabledCells.toMutableSet()
        val maxIdx = size - 1

        when (direction) {
            GravityDirection.DOWN -> {
                for (col in 0 until size) {
                    var writeRow = maxIdx
                    for (row in maxIdx downTo 0) {
                        val idx = row * size + col
                        if (disabledCells.contains(idx)) {
                            writeRow = row - 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeRow >= 0 && disabledCells.contains(writeRow * size + col)) {
                                writeRow--
                            }
                            if (writeRow >= 0) {
                                newCells[writeRow * size + col] = cells[idx]
                                writeRow--
                            }
                        }
                    }
                }
            }
            GravityDirection.UP -> {
                for (col in 0 until size) {
                    var writeRow = 0
                    for (row in 0..maxIdx) {
                        val idx = row * size + col
                        if (disabledCells.contains(idx)) {
                            writeRow = row + 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeRow <= maxIdx && disabledCells.contains(writeRow * size + col)) {
                                writeRow++
                            }
                            if (writeRow <= maxIdx) {
                                newCells[writeRow * size + col] = cells[idx]
                                writeRow++
                            }
                        }
                    }
                }
            }
            GravityDirection.LEFT -> {
                for (row in 0 until size) {
                    var writeCol = 0
                    for (col in 0..maxIdx) {
                        val idx = row * size + col
                        if (disabledCells.contains(idx)) {
                            writeCol = col + 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeCol <= maxIdx && disabledCells.contains(row * size + writeCol)) {
                                writeCol++
                            }
                            if (writeCol <= maxIdx) {
                                newCells[row * size + writeCol] = cells[idx]
                                writeCol++
                            }
                        }
                    }
                }
            }
            GravityDirection.RIGHT -> {
                for (row in 0 until size) {
                    var writeCol = maxIdx
                    for (col in maxIdx downTo 0) {
                        val idx = row * size + col
                        if (disabledCells.contains(idx)) {
                            writeCol = col - 1
                            continue
                        }
                        if (cells[idx] != Player.NONE) {
                            while (writeCol >= 0 && disabledCells.contains(row * size + writeCol)) {
                                writeCol--
                            }
                            if (writeCol >= 0) {
                                newCells[row * size + writeCol] = cells[idx]
                                writeCol--
                            }
                        }
                    }
                }
            }
        }
        return copy(cells = newCells, disabledCells = newDisabled)
    }

    fun applyDrift(direction: DriftDirection, strength: Int = 1): Board {
        var result = this
        for (i in 0 until strength) {
            result = result.applyDriftStep(direction)
        }
        return result
    }

    private fun applyDriftStep(direction: DriftDirection): Board {
        val newCells = cells.copyOf()
        val center = size / 2
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val idx = row * size + col
                if (cells[idx] == Player.NONE || disabledCells.contains(idx)) continue
                
                val (targetRow, targetCol) = when (direction) {
                    DriftDirection.TO_CENTER -> {
                        val dr = if (row < center) 1 else if (row > center) -1 else 0
                        val dc = if (col < center) 1 else if (col > center) -1 else 0
                        Pair(row + dr, col + dc)
                    }
                    DriftDirection.TO_EDGES -> {
                        val dr = if (row < center) -1 else if (row > center) 1 else 0
                        val dc = if (col < center) -1 else if (col > center) 1 else 0
                        Pair(row + dr, col + dc)
                    }
                }
                
                if (targetRow in 0 until size && targetCol in 0 until size) {
                    val targetIdx = targetRow * size + targetCol
                    if (cells[targetIdx] == Player.NONE && !disabledCells.contains(targetIdx)) {
                        newCells[targetIdx] = cells[idx]
                        newCells[idx] = Player.NONE
                    }
                }
            }
        }
        return copy(cells = newCells)
    }

    fun resize(newSize: Int, newWinCondition: Int = newSize): Board {
        val newCells = Array(newSize * newSize) { Player.NONE }
        val newDisabled = mutableSetOf<Int>()
        val newFrozen = mutableSetOf<Int>()
        
        val copySize = minOf(size, newSize)
        for (row in 0 until copySize) {
            for (col in 0 until copySize) {
                val oldIdx = row * size + col
                val newIdx = row * newSize + col
                newCells[newIdx] = cells[oldIdx]
                if (disabledCells.contains(oldIdx)) newDisabled.add(newIdx)
                if (frozenCells.contains(oldIdx)) newFrozen.add(newIdx)
            }
        }
        return Board(size = newSize, winCondition = newWinCondition, cells = newCells, 
            disabledCells = newDisabled, frozenCells = newFrozen)
    }

    fun copy(): Board = copy(cells = cells.copyOf())

    fun toArray(): Array<Player> = cells.copyOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false
        return size == other.size && winCondition == other.winCondition &&
            cells.contentEquals(other.cells) && disabledCells == other.disabledCells
    }

    override fun hashCode(): Int {
        var result = size
        result = 31 * result + winCondition
        result = 31 * result + cells.contentHashCode()
        result = 31 * result + disabledCells.hashCode()
        return result
    }

    companion object {
        fun create(size: Int = 3, winCondition: Int = size): Board = Board(size = size, winCondition = winCondition)
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

enum class RotationRegion {
    FULL, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

enum class SlideDirection {
    UP, DOWN, LEFT, RIGHT
}

enum class DriftDirection {
    TO_CENTER, TO_EDGES
}

enum class TrapType {
    SWAP_MARKS,
    REMOVE_LAST_MOVE,
    FREEZE_ADJACENT,
    DISABLE_CELL
}

data class DelayedPlacement(
    val cell: Cell,
    val player: Player,
    val placedTurn: Int,
    val revealTurn: Int
)
