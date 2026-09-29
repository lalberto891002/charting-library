package com.charting.charts.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

data class ChartValueId(val series: String, val point: String)
data class AnimatedChartValue(val x: Double, val y: Double)

/** Implement this interface for reveal, spring or application-specific animation behavior. */
interface ChartAnimationStrategy {
    @Composable
    fun values(target: Map<ChartValueId, AnimatedChartValue>): Map<ChartValueId, AnimatedChartValue>
}

object NoChartAnimation : ChartAnimationStrategy {
    @Composable
    override fun values(target: Map<ChartValueId, AnimatedChartValue>) = target
}

/** Interrupted updates continue from the last displayed frame. Removed IDs disappear immediately. */
class GrowingChartAnimation(
    private val specification: FiniteAnimationSpec<Float> = tween(600),
) : ChartAnimationStrategy {
    @Composable
    override fun values(target: Map<ChartValueId, AnimatedChartValue>): Map<ChartValueId, AnimatedChartValue> {
        var displayed by remember { mutableStateOf(target.mapValues { (_, value) -> value.copy(y = 0.0) }) }
        LaunchedEffect(target, specification) {
            val start = target.mapValues { (id, value) -> displayed[id] ?: value.copy(y = 0.0) }
            animate(0f, 1f, animationSpec = specification) { fraction, _ ->
                // Clamp overshooting specifications: pie values must never become negative.
                val t = fraction.coerceIn(0f, 1f).toDouble()
                displayed = target.mapValues { (id, end) ->
                    val begin = start.getValue(id)
                    AnimatedChartValue(begin.x * (1 - t) + end.x * t, begin.y * (1 - t) + end.y * t)
                }
            }
            displayed = target
        }
        return target.mapValues { (id, value) -> displayed[id] ?: value.copy(y = 0.0) }
    }
}

val DefaultChartAnimation: ChartAnimationStrategy = GrowingChartAnimation()
