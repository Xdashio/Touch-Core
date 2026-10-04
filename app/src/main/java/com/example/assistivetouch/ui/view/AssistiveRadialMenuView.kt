package com.example.assistivetouch.ui.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.assistivetouch.R
import com.example.assistivetouch.model.AssistiveItem
import com.example.assistivetouch.model.MenuPage
import com.example.assistivetouch.model.MenuSlot
import kotlin.math.min

/**
 * A custom radial menu view that dynamically positions menu items around a central axis
 * mirroring Apple's AssistiveTouch layout.
 *
 * All icons, titles, and actions are data-driven via MenuPage and AssistiveItem.
 */
class AssistiveRadialMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var currentPage: MenuPage? = null
    private var onItemClickListener: ((AssistiveItem) -> Unit)? = null

    // Map each child View to its AssistiveItem
    private val itemViews = mutableMapOf<View, AssistiveItem>()

    fun setOnItemClickListener(listener: (AssistiveItem) -> Unit) {
        this.onItemClickListener = listener
    }

    /**
     * Updates the displayed page. Supports silky-smooth transitions between pages.
     */
    private val viewPool = ArrayDeque<View>()

    fun setMenuPage(page: MenuPage, animate: Boolean = true) {
        if (!animate || childCount == 0) {
            currentPage = page
            renderPage(page)
            return
        }

        // Animate out existing items with hardware layers for 60fps smoothness
        val animators = mutableListOf<android.animation.Animator>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            val fadeOut = ObjectAnimator.ofFloat(child, View.ALPHA, child.alpha, 0f)
            val scaleX = ObjectAnimator.ofFloat(child, View.SCALE_X, child.scaleX, 0.7f)
            val scaleY = ObjectAnimator.ofFloat(child, View.SCALE_Y, child.scaleY, 0.7f)
            animators.add(fadeOut)
            animators.add(scaleX)
            animators.add(scaleY)
        }

        AnimatorSet().apply {
            playTogether(animators)
            duration = 120
            interpolator = DecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentPage = page
                    renderPage(page)
                    animateInNewItems()
                }
            })
            start()
        }
    }

    private fun renderPage(page: MenuPage) {
        // Recycle current child views into viewPool to prevent GC stalls
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.setLayerType(View.LAYER_TYPE_NONE, null)
            viewPool.add(child)
        }
        removeAllViews()
        itemViews.clear()

        val inflater = LayoutInflater.from(context)

        // Radial items
        for (item in page.items) {
            val view = obtainOrInflateItemView(inflater)
            bindItemView(view, item)
            itemViews[view] = item
            addView(view)
        }

        // Center item (e.g. Back button in submenus)
        page.centerItem?.let { centerItem ->
            val view = obtainOrInflateItemView(inflater)
            bindItemView(view, centerItem)
            itemViews[view] = centerItem
            addView(view)
        }

        requestLayout()
    }

    private fun obtainOrInflateItemView(inflater: LayoutInflater): View {
        return if (viewPool.isNotEmpty()) {
            val recycled = viewPool.removeFirst()
            recycled.alpha = 1f
            recycled.scaleX = 1f
            recycled.scaleY = 1f
            recycled
        } else {
            inflater.inflate(R.layout.item_assistive_touch, this, false)
        }
    }

    private fun animateInNewItems() {
        val animators = mutableListOf<android.animation.Animator>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            child.alpha = 0f
            child.scaleX = 0.6f
            child.scaleY = 0.6f

            val fadeIn = ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f)
            val scaleX = ObjectAnimator.ofFloat(child, View.SCALE_X, 0.6f, 1f)
            val scaleY = ObjectAnimator.ofFloat(child, View.SCALE_Y, 0.6f, 1f)
            animators.add(fadeIn)
            animators.add(scaleX)
            animators.add(scaleY)
        }

        AnimatorSet().apply {
            playTogether(animators)
            duration = 220
            interpolator = OvershootInterpolator(1.2f)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    for (i in 0 until childCount) {
                        getChildAt(i).setLayerType(View.LAYER_TYPE_NONE, null)
                    }
                }
            })
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindItemView(view: View, item: AssistiveItem) {
        val iconView = view.findViewById<ImageView>(R.id.itemIcon)
        val labelView = view.findViewById<TextView>(R.id.itemLabel)

        // Bind icon
        if (item.iconDrawable != null) {
            iconView.setImageDrawable(item.iconDrawable)
            iconView.clearColorFilter()
        } else if (item.iconRes != null) {
            iconView.setImageResource(item.iconRes)
            iconView.setColorFilter(Color.WHITE)
        } else {
            iconView.setImageDrawable(null)
        }

        // Bind title
        val title = item.getDisplayTitle(context)
        labelView.text = title

        // Spring touch feedback
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(80).start()
                    false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    false
                }
                else -> false
            }
        }

        view.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // Measure children
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val cx = width / 2f
        val cy = height / 2f

        // Standard Apple AssistiveTouch radial radius
        // Provides optimal spacing for 6 or 8 icons around the center
        val availableRadius = min(width, height) / 2f
        val radialRadius = availableRadius * 0.69f

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val item = itemViews[child] ?: continue

            val childW = child.measuredWidth
            val childH = child.measuredHeight

            if (item.slot == MenuSlot.CENTER) {
                val left = (cx - childW / 2f).toInt()
                val top = (cy - childH / 2f).toInt()
                child.layout(left, top, left + childW, top + childH)
            } else {
                val (offsetX, offsetY) = item.slot.computeOffset(radialRadius)
                val left = (cx + offsetX - childW / 2f).toInt()
                val top = (cy + offsetY - childH / 2f).toInt()
                child.layout(left, top, left + childW, top + childH)
            }
        }
    }
}
