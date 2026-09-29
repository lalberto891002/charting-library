package com.charting.charts.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.charting.charts.animation.AnimatedChartValue
import com.charting.charts.animation.ChartAnimationStrategy
import com.charting.charts.animation.ChartValueId
import com.charting.charts.animation.DefaultChartAnimation
import com.charting.charts.core.ChartLabel
import com.charting.charts.core.ChartLayer
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.ChartRenderer
import com.charting.charts.core.ChartScene
import com.charting.charts.core.ChartSurface
import com.charting.charts.core.PolygonObstacle
import com.charting.charts.core.pieFractions
import com.charting.charts.labels.LabelPlacementStrategy
import com.charting.charts.labels.StaggeredLabelPlacement
import com.charting.charts.model.PieSlice
import com.charting.charts.style.FillPaint
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LocalChartTheme
import com.charting.charts.style.PieStyle
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    style: PieStyle = LocalChartTheme.current.pie,
    labelStyle: LabelStyle = LocalChartTheme.current.labels,
    labelPlacement: LabelPlacementStrategy = StaggeredLabelPlacement,
    animation: ChartAnimationStrategy = DefaultChartAnimation,
    labelContent: (@Composable (ChartLabel) -> Unit)? = null,
    contentDescription: String? = null,
) {
    require(slices.map { it.id }.distinct().size == slices.size) { "Slice IDs must be unique" }
    val theme = LocalChartTheme.current
    val entranceId = ChartValueId("animation", "entrance")
    val target = slices.associate { ChartValueId("pie", it.id) to AnimatedChartValue(0.0, it.value) } +
        (entranceId to AnimatedChartValue(0.0, 1.0))
    val displayed = animation.values(target)
    val fractions = pieFractions(slices.map { displayed.getValue(ChartValueId("pie", it.id)).y })
    val entrance = displayed.getValue(entranceId).y.toFloat().coerceIn(0f, 1f)
    val description = contentDescription ?: if (slices.isEmpty() || slices.all { it.value == 0.0 }) "Pie chart. No data." else
        "Pie chart. " + slices.joinToString { "${it.label}: ${it.value}" }
    ChartSurface(ChartRenderer { context ->
        with(context.density) {
            val padding = 16.dp.toPx()
            val top = if (labelStyle.visible) labelStyle.reservedTopSpace.toPx().coerceAtMost(context.height * .5f) else padding
            val radius = (min(context.width - padding * 2, context.height - top - padding) / 2).coerceAtLeast(0f)
            val center = ChartPosition(context.width / 2, top + radius)
            fun point(angle: Float, r: Float): ChartPosition {
                val radians = Math.toRadians(angle.toDouble())
                return ChartPosition(center.x + cos(radians).toFloat() * r, center.y + sin(radians).toFloat() * r)
            }
            var angle = style.startAngle
            val layers = mutableListOf<ChartLayer>()
            val obstacles = mutableListOf<PolygonObstacle>()
            val labels = mutableListOf<ChartLabel>()
            slices.forEachIndexed { index, slice ->
                val sweep = fractions[index] * 360f * entrance
                if (sweep > 0f) {
                    val steps = ceil(sweep / 2f).toInt().coerceAtLeast(1)
                    val angles = (0..steps).map { angle + sweep * it / steps }
                    val outer = angles.map { point(it, radius) }
                    val inner = if (style.holeFraction == 0f) listOf(center) else angles.reversed().map { point(it, radius * style.holeFraction) }
                    val polygon = outer + inner
                    val path = polygon.asPath(closed = true)
                    val fill = slice.fill ?: FillPaint(theme.palette[index % theme.palette.size])
                    layers += ChartLayer { drawPath(path, fill.brush, alpha = fill.alpha) }
                    obstacles += PolygonObstacle(polygon)
                    labels += ChartLabel(slice.id, slice.label, point(angle + sweep / 2, radius + labelStyle.gap.toPx()))
                }
                angle += sweep
            }
            ChartScene(layers, obstacles, labels)
        }
    }, description, modifier, labelStyle, labelPlacement, labelContent)
}
