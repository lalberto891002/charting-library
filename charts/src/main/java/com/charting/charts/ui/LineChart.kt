package com.charting.charts.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.charting.charts.animation.AnimatedChartValue
import com.charting.charts.animation.ChartAnimationStrategy
import com.charting.charts.animation.ChartValueId
import com.charting.charts.animation.DefaultChartAnimation
import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartLabel
import com.charting.charts.core.ChartLayer
import com.charting.charts.core.ChartObstacle
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.ChartRenderer
import com.charting.charts.core.ChartScene
import com.charting.charts.core.ChartSurface
import com.charting.charts.core.PolygonObstacle
import com.charting.charts.core.PolylineObstacle
import com.charting.charts.core.RectangleObstacle
import com.charting.charts.core.lineSegment
import com.charting.charts.core.resolveSectionOwners
import com.charting.charts.labels.LabelPlacementStrategy
import com.charting.charts.labels.StaggeredLabelPlacement
import com.charting.charts.model.ChartRange
import com.charting.charts.model.ChartSeries
import com.charting.charts.style.AxisStyle
import com.charting.charts.style.FillBaseline
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LineChartStyle
import com.charting.charts.style.LineInterpolation
import com.charting.charts.style.LocalChartTheme

@Composable
fun LineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    style: LineChartStyle? = null,
    xRange: ChartRange? = null,
    yRange: ChartRange? = null,
    axes: AxisStyle = LocalChartTheme.current.axes,
    labelStyle: LabelStyle = LocalChartTheme.current.labels,
    labelPlacement: LabelPlacementStrategy = StaggeredLabelPlacement,
    animation: ChartAnimationStrategy = DefaultChartAnimation,
    labelContent: (@Composable (ChartLabel) -> Unit)? = null,
    contentDescription: String? = null,
) {
    require(series.map { it.id }.distinct().size == series.size) { "Series IDs must be unique" }
    val theme = LocalChartTheme.current
    val styles = series.mapIndexed { index, item ->
        item.style ?: style ?: theme.line.copy(
            color = theme.palette[index % theme.palette.size],
            dots = theme.line.dots.copy(color = theme.palette[index % theme.palette.size]),
        )
    }
    val target = series.flatMap { item -> item.points.map { ChartValueId(item.id, it.id) to AnimatedChartValue(it.x, it.y) } }.toMap()
    val displayed = animation.values(target)
    val resolvedX = xRange ?: ChartRange.enclosing(target.values.map { it.x } + displayed.values.map { it.x })
    val bases = styles.mapNotNull { (it.fill.baseline as? FillBaseline.Value)?.y }
    val resolvedY = yRange ?: ChartRange.enclosing(target.values.map { it.y } + displayed.values.map { it.y } + bases, includeZero = true)
    val owners = series.mapIndexed { index, item ->
        resolveSectionOwners(item.points.map { it.id }, styles[index].fill.sections.map { it.startPointId to it.endPointId })
    }
    val description = contentDescription ?: if (target.isEmpty()) "Line chart. No data." else
        "Line chart. " + series.joinToString("; ") { item ->
            item.name + ": " + item.points.joinToString { "${it.label ?: it.id}: x=${it.x}, y=${it.y}" }
        }
    ChartSurface(ChartRenderer { context ->
        with(context.density) {
            val frame = cartesianFrame(context, resolvedX, resolvedY, axes)
            val plot = frame.scale.plot
            val fills = mutableListOf<ChartLayer>()
            val lines = mutableListOf<ChartLayer>()
            val dots = mutableListOf<ChartLayer>()
            val obstacles = mutableListOf<ChartObstacle>()
            val labels = mutableListOf<ChartLabel>()
            series.forEachIndexed { seriesIndex, item ->
                val lineStyle = styles[seriesIndex]
                val positions = item.points.map { point ->
                    val value = displayed.getValue(ChartValueId(item.id, point.id))
                    ChartPosition(frame.scale.x(value.x), frame.scale.y(value.y))
                }
                val segments = positions.zipWithNext().map { (a, b) -> lineSegment(a, b, lineStyle.interpolation == LineInterpolation.Smooth) }
                val baseline = when (val base = lineStyle.fill.baseline) {
                    FillBaseline.Bottom -> plot.bottom
                    FillBaseline.Zero -> frame.scale.y(0.0)
                    is FillBaseline.Value -> frame.scale.y(base.y)
                }.coerceIn(plot.top, plot.bottom)
                val paints = owners[seriesIndex].map { owner -> owner?.let { lineStyle.fill.sections[it].paint } ?: lineStyle.fill.full }
                var edge = 0
                while (edge < segments.size) {
                    val paint = paints[edge]
                    var end = edge + 1
                    while (end < segments.size && paints[end] == paint) end++
                    if (paint != null && paint.alpha > 0f) {
                        val curve = segments.subList(edge, end).flatMapIndexed { index, points -> if (index == 0) points else points.drop(1) }
                        val polygon = curve + ChartPosition(curve.last().x, baseline) + ChartPosition(curve.first().x, baseline)
                        val path = polygon.asPath(closed = true)
                        fills += ChartLayer {
                            clipRect(plot.left, plot.top, plot.right, plot.bottom) { drawPath(path, paint.brush, alpha = paint.alpha) }
                        }
                        obstacles += PolygonObstacle(polygon)
                    }
                    edge = end
                }
                val trajectory = segments.flatMapIndexed { index, points -> if (index == 0) points else points.drop(1) }
                val path = trajectory.asPath()
                val width = lineStyle.width.toPx()
                val effect = lineStyle.dash.takeIf { it.isNotEmpty() }?.let { values ->
                    PathEffect.dashPathEffect(values.map { it.toPx() }.toFloatArray())
                }
                lines += ChartLayer {
                    clipRect(plot.left, plot.top, plot.right, plot.bottom) {
                        drawPath(path, lineStyle.color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = effect))
                    }
                }
                obstacles += PolylineObstacle(trajectory, width)
                item.points.forEachIndexed { index, point ->
                    val position = positions[index]
                    val dotStyle = point.style ?: lineStyle.dots
                    val radius = dotStyle.radius.toPx()
                    if (dotStyle.visible && plot.contains(position)) {
                        dots += ChartLayer { drawCircle(dotStyle.color, radius, position.offset()) }
                        obstacles += RectangleObstacle(ChartBounds(position.x - radius, position.y - radius, position.x + radius, position.y + radius))
                    }
                    if (point.label != null && plot.contains(position)) {
                        labels += ChartLabel("${item.id.length}:${item.id}${point.id}", point.label, position, point.labelPriority)
                    }
                }
            }
            ChartScene(listOf(frame.layer) + fills + lines + dots, frame.obstacles + obstacles, labels)
        }
    }, description, modifier, labelStyle, labelPlacement, labelContent)
}
