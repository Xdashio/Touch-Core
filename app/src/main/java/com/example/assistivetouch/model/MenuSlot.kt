package com.example.assistivetouch.model

import kotlin.math.cos
import kotlin.math.sin

/**
 * Standard slot positions in an AssistiveTouch radial menu.
 * Angles in degrees: 0° is Right (3 o'clock), 90° is Bottom (6 o'clock),
 * 180° is Left (9 o'clock), 270° (-90°) is Top (12 o'clock).
 */
enum class MenuSlot(val angleDegrees: Double?) {
    TOP(-90.0),
    TOP_RIGHT(-45.0),
    RIGHT(0.0),
    BOTTOM_RIGHT(45.0),
    BOTTOM(90.0),
    BOTTOM_LEFT(135.0),
    LEFT(180.0),
    TOP_LEFT(-135.0),
    CENTER(null);

    /**
     * Compute relative (x, y) offset from the center given a radius R.
     */
    fun computeOffset(radius: Float): Pair<Float, Float> {
        val angle = angleDegrees ?: return Pair(0f, 0f)
        val rad = Math.toRadians(angle)
        val x = (radius * cos(rad)).toFloat()
        val y = (radius * sin(rad)).toFloat()
        return Pair(x, y)
    }
}
