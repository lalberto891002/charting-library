package com.charting.charts.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.charting.charts.animation.AnimatedChartValue
import com.charting.charts.animation.ChartAnimationStrategy
import com.charting.charts.animation.ChartValueId
import com.charting.charts.animation.DefaultChartAnimation
import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartLabel
import com.charting.charts.core.ChartLayer
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.ChartRenderer
import com.charting.charts.core.ChartScene
import com.charting.charts.core.ChartSurface
import com.charting.charts.core.RectangleObstacle
import com.charting.charts.labels.LabelPlacementStrategy
import com.charting.charts.labels.StaggeredLabelPlacement
import com.charting.charts.model.BarEntry
import com.charting.charts.model.ChartRange
import com.charting.charts.style.AxisStyle
import com.charting.charts.style.BarStyle
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LocalChartTheme
import kotlin.math.max
import kotlin.math.min

@Composable
fun BarChart(
    entries: List<BarEntry>,
    modifier: Modifier = Modifier,
    style: BarStyle = LocalChartTheme.current.bar,
    barWidthFraction: Float = 0.65f,
    yRange: ChartRange? = null,
    axes: AxisStyle = LocalChartTheme.current.axes,
    labelStyle: LabelStyle = LocalChartTheme.current.labels,
    labelPlacement: LabelPlacementStrategy = StaggeredLabelPlacement,
    animation: ChartAnimationStrategy = DefaultChartAnimation,
    labelContent: (@Composable (ChartLabel) -> Unit)? = null,
    contentDescription: String? = null,
) {
    require(entries.map { it.id }.distinct().size == entries.size) { "Bar IDs must be unique" }
    require(barWidthFraction.isFinite() && barWidthFraction > 0f && barWidthFraction <= 1f)
    val target = entries.mapIndexed { index, entry -> ChartValueId("bar", entry.id) to AnimatedChartValue(index.toDouble(), entry.value) }.toMap()
    val displayed = animation.values(target)
    val range = yRange ?: ChartRange.enclosing(entries.map { it.value } + displayed.values.map { it.y }, includeZero = true)
    val description = contentDescription ?: if (entries.isEmpty()) "Bar chart. No data." else
        "Bar chart. " + entries.joinToString { "${it.label}: ${it.value}" }
    ChartSurface(ChartRenderer { context ->
        val frame = cartesianFrame(context, ChartRange(-0.5, max(0.5, entries.size - 0.5)), range, axes, showXValues = false)
        val plot = frame.scale.plot
        val baseline = frame.scale.y(0.0).coerceIn(plot.top, plot.bottom)
        val width = plot.width / max(1, entries.size) * barWidthFraction
        val bounds = entries.map { entry ->
            val value = displayed.getValue(ChartValueId("bar", entry.id))
            val x = frame.scale.x(value.x).coerceIn(plot.left + width / 2, plot.right - width / 2)
            val y = frame.scale.y(value.y).coerceIn(plot.top, plot.bottom)
            ChartBounds(x - width / 2, min(y, baseline), x + width / 2, max(y, baseline))
        }
        val bars = entries.mapIndexed { index, entry ->
            val box = bounds[index]
            val paint = (entry.style ?: style).fill
            ChartLayer { drawRect(paint.brush, Offset(box.left, box.top), Size(box.width, box.height), alpha = paint.alpha) }
        }
        ChartScene(listOf(frame.layer) + bars, frame.obstacles + bounds.map(::RectangleObstacle), entries.mapIndexed { index, entry ->
            val box = bounds[index]
            ChartLabel(entry.id, "${entry.label}: ${axes.formatter(entry.value)}", ChartPosition((box.left + box.right) / 2, box.top), entry.labelPriority)
        })
    }, description, modifier, labelStyle, labelPlacement, labelContent)
}
