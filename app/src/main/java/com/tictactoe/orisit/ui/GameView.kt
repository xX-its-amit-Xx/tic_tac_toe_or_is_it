package com.tictactoe.orisit.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.tictactoe.orisit.R
import com.tictactoe.orisit.model.Board
import com.tictactoe.orisit.model.Cell
import com.tictactoe.orisit.model.GravityDirection
import com.tictactoe.orisit.model.ModEffect
import com.tictactoe.orisit.model.MutationPreview
import com.tictactoe.orisit.model.Player
import kotlin.math.min

/**
 * Custom view for rendering the Tic-Tac-Toe game board.
 * Handles touch input and animations for mod effects.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var board: Board = Board()
    private var onCellClicked: ((Cell) -> Unit)? = null
    private var inputEnabled = true

    // Animation state
    private var rotationAngle = 0f
    private var gravityOffset = FloatArray(9) { 0f }
    private var highlightedCells = setOf<Int>()
    private var gravityIndicator: GravityDirection? = null

    // Paints
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val xPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.player_x)
        strokeWidth = 12f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val oPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.player_o)
        strokeWidth = 12f
        style = Paint.Style.STROKE
    }

    private val disabledPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.tile_disabled)
        style = Paint.Style.FILL
    }

    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.highlight_warning)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.mod_gravity)
        style = Paint.Style.FILL
        strokeWidth = 8f
    }

    private var cellSize = 0f
    private var boardOffset = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val size = min(w, h)
        val padding = size * 0.08f
        cellSize = (size - padding * 2) / 3f
        boardOffset = padding
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        
        // Apply rotation animation
        if (rotationAngle != 0f) {
            val centerX = width / 2f
            val centerY = height / 2f
            canvas.rotate(rotationAngle, centerX, centerY)
        }

        drawGrid(canvas)
        drawDisabledCells(canvas)
        drawHighlightedCells(canvas)
        drawMarks(canvas)
        
        canvas.restore()

        // Draw gravity indicator (not rotated)
        drawGravityIndicator(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        // Vertical lines
        for (i in 1..2) {
            val x = boardOffset + i * cellSize
            canvas.drawLine(x, boardOffset, x, boardOffset + 3 * cellSize, gridPaint)
        }
        // Horizontal lines
        for (i in 1..2) {
            val y = boardOffset + i * cellSize
            canvas.drawLine(boardOffset, y, boardOffset + 3 * cellSize, y, gridPaint)
        }
    }

    private fun drawDisabledCells(canvas: Canvas) {
        for (idx in board.getDisabledCells()) {
            val row = idx / 3
            val col = idx % 3
            val rect = getCellRect(row, col)
            canvas.drawRect(rect, disabledPaint)
            
            // Draw X pattern on disabled cells
            val padding = cellSize * 0.2f
            canvas.drawLine(
                rect.left + padding, rect.top + padding,
                rect.right - padding, rect.bottom - padding,
                gridPaint
            )
            canvas.drawLine(
                rect.right - padding, rect.top + padding,
                rect.left + padding, rect.bottom - padding,
                gridPaint
            )
        }
    }

    private fun drawHighlightedCells(canvas: Canvas) {
        for (idx in highlightedCells) {
            val row = idx / 3
            val col = idx % 3
            val rect = getCellRect(row, col)
            val inset = 4f
            rect.inset(inset, inset)
            canvas.drawRect(rect, highlightPaint)
        }
    }

    private fun drawMarks(canvas: Canvas) {
        val cells = board.toArray()
        for (i in cells.indices) {
            val player = cells[i]
            if (player == Player.NONE) continue

            val row = i / 3
            val col = i % 3
            val rect = getCellRect(row, col)
            
            // Apply gravity animation offset
            val offset = gravityOffset[i]
            rect.offset(0f, offset)

            val padding = cellSize * 0.2f
            when (player) {
                Player.X -> drawX(canvas, rect, padding)
                Player.O -> drawO(canvas, rect, padding)
                else -> {}
            }
        }
    }

    private fun drawX(canvas: Canvas, rect: RectF, padding: Float) {
        canvas.drawLine(
            rect.left + padding, rect.top + padding,
            rect.right - padding, rect.bottom - padding,
            xPaint
        )
        canvas.drawLine(
            rect.right - padding, rect.top + padding,
            rect.left + padding, rect.bottom - padding,
            xPaint
        )
    }

    private fun drawO(canvas: Canvas, rect: RectF, padding: Float) {
        val radius = (rect.width() - padding * 2) / 2
        canvas.drawCircle(rect.centerX(), rect.centerY(), radius, oPaint)
    }

    private fun drawGravityIndicator(canvas: Canvas) {
        val dir = gravityIndicator ?: return
        
        val arrowSize = 40f
        val margin = 20f
        val centerX = width / 2f
        val centerY = height / 2f
        val boardEnd = boardOffset + 3 * cellSize

        val path = Path()
        when (dir) {
            GravityDirection.DOWN -> {
                val y = boardEnd + margin
                path.moveTo(centerX, y + arrowSize)
                path.lineTo(centerX - arrowSize / 2, y)
                path.lineTo(centerX + arrowSize / 2, y)
                path.close()
            }
            GravityDirection.UP -> {
                val y = boardOffset - margin
                path.moveTo(centerX, y - arrowSize)
                path.lineTo(centerX - arrowSize / 2, y)
                path.lineTo(centerX + arrowSize / 2, y)
                path.close()
            }
            GravityDirection.LEFT -> {
                val x = boardOffset - margin
                path.moveTo(x - arrowSize, centerY)
                path.lineTo(x, centerY - arrowSize / 2)
                path.lineTo(x, centerY + arrowSize / 2)
                path.close()
            }
            GravityDirection.RIGHT -> {
                val x = boardEnd + margin
                path.moveTo(x + arrowSize, centerY)
                path.lineTo(x, centerY - arrowSize / 2)
                path.lineTo(x, centerY + arrowSize / 2)
                path.close()
            }
        }
        canvas.drawPath(path, arrowPaint)
    }

    private fun getCellRect(row: Int, col: Int): RectF {
        val left = boardOffset + col * cellSize
        val top = boardOffset + row * cellSize
        return RectF(left, top, left + cellSize, top + cellSize)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!inputEnabled) return false
        
        if (event.action == MotionEvent.ACTION_UP) {
            val cell = getCellFromTouch(event.x, event.y)
            if (cell != null && board.isEmpty(cell)) {
                onCellClicked?.invoke(cell)
            }
        }
        return true
    }

    private fun getCellFromTouch(x: Float, y: Float): Cell? {
        val col = ((x - boardOffset) / cellSize).toInt()
        val row = ((y - boardOffset) / cellSize).toInt()
        
        if (row in 0..2 && col in 0..2) {
            return Cell(row, col)
        }
        return null
    }

    // Public API

    fun setBoard(newBoard: Board) {
        board = newBoard
        invalidate()
    }

    fun setOnCellClickListener(listener: (Cell) -> Unit) {
        onCellClicked = listener
    }

    fun setInputEnabled(enabled: Boolean) {
        inputEnabled = enabled
    }

    fun setHighlightedCells(cells: Set<Int>) {
        highlightedCells = cells
        invalidate()
    }

    fun setGravityIndicator(direction: GravityDirection?) {
        gravityIndicator = direction
        invalidate()
    }

    // Animations

    fun animateRotation(angle: Int, clockwise: Boolean, onComplete: () -> Unit) {
        val targetAngle = if (clockwise) angle.toFloat() else -angle.toFloat()
        
        ValueAnimator.ofFloat(0f, targetAngle).apply {
            duration = 400
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                rotationAngle = animator.animatedValue as Float
                invalidate()
            }
            doOnEnd {
                rotationAngle = 0f
                onComplete()
            }
            start()
        }
    }

    fun animateGravity(direction: GravityDirection, onComplete: () -> Unit) {
        val offset = when (direction) {
            GravityDirection.DOWN -> cellSize
            GravityDirection.UP -> -cellSize
            else -> 0f
        }
        
        ValueAnimator.ofFloat(0f, offset, 0f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                for (i in gravityOffset.indices) {
                    gravityOffset[i] = value
                }
                invalidate()
            }
            doOnEnd {
                gravityOffset.fill(0f)
                onComplete()
            }
            start()
        }
    }

    fun animateMutation(onComplete: () -> Unit) {
        // Flash highlight animation
        ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = 400
            addUpdateListener {
                highlightPaint.alpha = ((it.animatedValue as Float) * 255).toInt()
                invalidate()
            }
            doOnEnd {
                highlightPaint.alpha = 255
                highlightedCells = emptySet()
                onComplete()
            }
            start()
        }
    }

    private inline fun ValueAnimator.doOnEnd(crossinline action: () -> Unit) {
        addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) { action() }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }
}
