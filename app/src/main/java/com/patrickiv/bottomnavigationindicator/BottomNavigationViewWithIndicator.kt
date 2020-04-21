package com.patrickiv.bottomnavigationindicator

import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.math.abs

private const val DEFAULT_SIZE_DP = 4
private const val BOTTOM_MARGIN_DP = 6
private const val DEFAULT_SCALE = 1f
private const val MAX_SCALE = 15f
private const val BASE_DURATION = 300L
private const val VARIABLE_DURATION = 300L

class BottomNavigationViewWithIndicator: BottomNavigationView,
    BottomNavigationView.OnNavigationItemSelectedListener {

    private var externalSelectedListener: OnNavigationItemSelectedListener? = null

    private var animator: ValueAnimator? = null
    private val evaluator = FloatEvaluator()

    private val indicator = RectF()
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorAccent)
    }

    private val bottomOffset = BOTTOM_MARGIN_DP * resources.displayMetrics.density
    private val defaultSize = DEFAULT_SIZE_DP * resources.displayMetrics.density
    private val radius = defaultSize / 2f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    init {
        super.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (externalSelectedListener?.onNavigationItemSelected(item) != false) {
            onItemSelected(item.itemId)
            return true
        }
        return false
    }

    override fun setOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener?) {
        externalSelectedListener = listener
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doOnPreDraw {
            // Move the indicator in place when the view is laid out
            onItemSelected(selectedItemId, false)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Clean up the animator if the view is going away
        cancelAnimator()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (isLaidOut) canvas.drawRoundRect(indicator, radius, radius, accentPaint)
    }

    private fun onItemSelected(itemId: Int, animate: Boolean = true) {
        if (!isLaidOut) return

        cancelAnimator()

        val itemView = findViewById<View>(itemId) ?: return
        val fromCenterX = indicator.centerX()
        val currentScale = indicator.width() / defaultSize

        val distance = abs(fromCenterX - (itemView.left + itemView.width / 2f))
        val animationDuration = if (animate) calculateDuration(distance) else 0L

        animator = ValueAnimator.ofFloat(currentScale, MAX_SCALE, DEFAULT_SCALE).apply {
            addUpdateListener {
                val scale = it.animatedValue as Float
                val indicatorWidth = defaultSize * scale

                val itemViewCenterX = itemView.left + itemView.width / 2f
                val distanceTravelled = interpolate(animatedFraction, fromCenterX, itemViewCenterX)

                val left = distanceTravelled - indicatorWidth / 2f
                val right = distanceTravelled + indicatorWidth / 2f
                val bottom = height - bottomOffset
                val top = bottom - defaultSize

                indicator.set(left, top, right, bottom)
                invalidate()
            }

            interpolator = LinearOutSlowInInterpolator()
            duration = animationDuration

            start()
        }
    }

    private fun calculateDuration(distance: Float) =
        (BASE_DURATION + VARIABLE_DURATION * distance / width.toFloat()).toLong()

    private fun interpolate(t: Float, a: Float, b: Float) = evaluator.evaluate(t, a, b)

    private fun cancelAnimator() = animator?.let {
        it.end()
        it.removeAllUpdateListeners()
        animator = null
    }
}
