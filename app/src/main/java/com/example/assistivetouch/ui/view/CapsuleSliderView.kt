package com.example.assistivetouch.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt

/**
 * A luxury custom capsule slider component designed to replace generic Material sliders.
 * Features:
 * - Fluid horizontal progress fill with rounded pill geometry
 * - Embedded start icon with dynamic contrast adaptation
 * - Live formatted value / percentage readout
 * - Tactile haptic feedback on discrete step changes and touch down
 * - Pure Black & White minimalist luxury aesthetic
 */
class CapsuleSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1C1C1E")
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * density
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 12f * density
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }

    private val clipPath = Path()
    private val backgroundRect = RectF()
    private val progressRect = RectF()

    // Configurable values
    var minValue: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var maxValue: Int = 100
        set(value) {
            field = value.coerceAtLeast(minValue + 1)
            invalidate()
        }

    var stepSize: Int = 1
        set(value) {
            field = value.coerceAtLeast(1)
        }

    var showPercentage: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var customFormat: ((Int) -> String)? = null
        set(value) {
            field = value
            invalidate()
        }

    private var currentProgress: Float = 0.5f // 0f..1f
    private var lastHapticValue: Int = -1

    var onValueChanged: ((value: Int, fromUser: Boolean) -> Unit)? = null
    var onProgressChanged: ((progress: Float, fromUser: Boolean) -> Unit)? = null

    private var iconDrawable: Drawable? = null
    private val iconSize = (20 * density).toInt()
    private val iconPadding = (16 * density).toInt()
    private val textPadding = (18 * density).toInt()

    private var isDragging = false

    val value: Int
        get() = (minValue + currentProgress * (maxValue - minValue)).roundToInt()

    fun setValue(newValue: Int, animate: Boolean = false) {
        val clamped = newValue.coerceIn(minValue, maxValue)
        val targetProgress = if (maxValue > minValue) {
            (clamped - minValue).toFloat() / (maxValue - minValue)
        } else 0f

        if (animate) {
            ValueAnimator.ofFloat(currentProgress, targetProgress).apply {
                duration = 180
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    currentProgress = va.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            currentProgress = targetProgress
            invalidate()
        }
    }

    fun setProgress(progress: Float, fromUser: Boolean = false) {
        val clamped = progress.coerceIn(0f, 1f)
        if (currentProgress != clamped) {
            currentProgress = clamped
            invalidate()
            val computedValue = value
            if (fromUser && computedValue != lastHapticValue) {
                lastHapticValue = computedValue
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
            onProgressChanged?.invoke(currentProgress, fromUser)
            onValueChanged?.invoke(computedValue, fromUser)
        }
    }

    fun setIconResource(@DrawableRes resId: Int) {
        iconDrawable = ContextCompat.getDrawable(context, resId)?.mutate()
        invalidate()
    }

    fun setIcon(drawable: Drawable?) {
        iconDrawable = drawable?.mutate()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultHeight = (48 * density).toInt()
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val resolvedHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(defaultHeight, heightSize)
            else -> defaultHeight
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, resolvedHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val halfBorder = borderPaint.strokeWidth / 2f
        backgroundRect.set(halfBorder, halfBorder, w - halfBorder, h - halfBorder)

        clipPath.reset()
        clipPath.addRect(backgroundRect, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val w = width.toFloat()

        // Draw background rectangle (absolute 0 radius)
        canvas.drawRect(backgroundRect, backgroundPaint)

        // Draw progress fill clipped to rectangular boundary
        canvas.save()
        canvas.clipPath(clipPath)

        val fillWidth = w * currentProgress
        progressRect.set(0f, 0f, fillWidth, h)
        canvas.drawRect(progressRect, progressPaint)

        // Draw start icon
        iconDrawable?.let { icon ->
            val iconLeft = iconPadding
            val iconTop = ((h - iconSize) / 2f).toInt()
            val iconRight = iconLeft + iconSize
            val iconBottom = iconTop + iconSize
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)

            // Adjust icon color: black when over white progress fill, white when over dark background
            val isOverProgress = fillWidth > (iconRight - iconSize / 2f)
            icon.setTint(if (isOverProgress) Color.BLACK else Color.WHITE)
            icon.draw(canvas)
        }

        // Draw formatted value text
        val displayText = customFormat?.invoke(value) ?: if (showPercentage) {
            "${(currentProgress * 100).roundToInt()}%"
        } else {
            "$value"
        }

        val bounds = Rect()
        textPaint.getTextBounds(displayText, 0, displayText.length, bounds)
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        val textX = w - textPadding - textWidth
        val textY = (h + textHeight) / 2f - 2f

        val isTextOverProgress = fillWidth > (textX + textWidth / 2f)
        textPaint.color = if (isTextOverProgress) Color.BLACK else Color.WHITE
        canvas.drawText(displayText, textX, textY, textPaint)

        canvas.restore()

        // Draw subtle border (absolute 0 radius)
        canvas.drawRect(backgroundRect, borderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                updateProgressFromTouch(event.x)
                animateTouchScale(0.97f)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    updateProgressFromTouch(event.x)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                    updateProgressFromTouch(event.x)
                    animateTouchScale(1.0f)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateProgressFromTouch(touchX: Float) {
        if (width <= 0) return
        val rawFraction = (touchX / width.toFloat()).coerceIn(0f, 1f)
        val range = maxValue - minValue
        if (range <= 0) return

        val unroundedValue = minValue + rawFraction * range
        val snappedValue = (unroundedValue / stepSize).roundToInt() * stepSize
        val clampedValue = snappedValue.coerceIn(minValue, maxValue)

        val finalFraction = (clampedValue - minValue).toFloat() / range
        setProgress(finalFraction, fromUser = true)
    }

    private fun animateTouchScale(targetScale: Float) {
        animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(90)
            .start()
    }
}
