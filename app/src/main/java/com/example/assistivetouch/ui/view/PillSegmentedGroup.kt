package com.example.assistivetouch.ui.view

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * A luxury segmented radio pill component designed to replace generic Android RadioButtons and RadioGroups.
 * Features:
 * - Fluid high-contrast Black & White aesthetic
 * - Smooth pill selection animations
 * - Discrete haptic feedback on selection
 * - Dynamic data binding with custom keys
 */
class PillSegmentedGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Segment(
        val id: String,
        val label: String,
        val iconRes: Int? = null
    )

    private val density = resources.displayMetrics.density
    private val segments = mutableListOf<Segment>()
    private val pillViews = mutableListOf<TextView>()
    private var selectedIndex = 0

    var onSegmentSelected: ((index: Int, id: String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        val paddingPx = (4 * density).toInt()
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        // Outer rectangular background (absolute 0 radius)
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 0f
            setColor(Color.parseColor("#161618"))
            setStroke((1 * density).toInt(), Color.parseColor("#2A2A2E"))
        }
    }

    fun setSegments(items: List<Segment>, initialIndex: Int = 0) {
        segments.clear()
        segments.addAll(items)
        removeAllViews()
        pillViews.clear()

        for (i in items.indices) {
            val item = items[i]
            val textView = TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f).apply {
                    marginEnd = if (i < items.size - 1) (2 * density).toInt() else 0
                }
                gravity = Gravity.CENTER
                text = item.label
                textSize = 13f
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    if (selectedIndex != i) {
                        selectSegment(i, notify = true)
                    }
                }
            }

            pillViews.add(textView)
            addView(textView)
        }

        selectedIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        updatePillStyles(animate = false)
    }

    fun selectSegment(index: Int, notify: Boolean = true) {
        if (index !in segments.indices) return
        selectedIndex = index

        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        updatePillStyles(animate = true)

        if (notify) {
            val segment = segments[index]
            onSegmentSelected?.invoke(index, segment.id)
        }
    }

    fun selectSegmentById(id: String, notify: Boolean = true) {
        val index = segments.indexOfFirst { it.id == id }
        if (index != -1) {
            selectSegment(index, notify)
        }
    }

    fun getSelectedIndex(): Int = selectedIndex

    fun getSelectedId(): String? = segments.getOrNull(selectedIndex)?.id

    private fun updatePillStyles(animate: Boolean) {
        for (i in pillViews.indices) {
            val tv = pillViews[i]
            val isSelected = i == selectedIndex

            val targetBgColor = if (isSelected) Color.WHITE else Color.TRANSPARENT
            val targetTextColor = if (isSelected) Color.BLACK else Color.parseColor("#8E8E93")
            val targetTypeface = if (isSelected) {
                android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            } else {
                android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            }

            tv.typeface = targetTypeface

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = 0f
                setColor(targetBgColor)
            }
            tv.background = bg
            tv.setTextColor(targetTextColor)

            if (animate && isSelected) {
                tv.scaleX = 0.94f
                tv.scaleY = 0.94f
                tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (48 * density).toInt()
        val heightSpec = MeasureSpec.makeMeasureSpec(desiredHeight, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
