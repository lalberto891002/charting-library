package com.charting.charts.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.charting.charts.core.CartesianScale
import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartLayer
import com.charting.charts.core.ChartObstacle
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.ChartRenderContext
import com.charting.charts.core.RectangleObstacle
import com.charting.charts.model.ChartRange
import com.charting.charts.style.AxisStyle
import kotlin.math.max
import kotlin.math.min

internal fun List<ChartPosition>.asPath(closed: Boolean = false) = Path().also { path ->
    firstOrNull()?.let { path.moveTo(it.x, it.y) }
    drop(1).forEach { path.lineTo(it.x, it.y) }
    if (closed) path.close()
}

internal fun ChartPosition.offset() = Offset(x, y)

internal data class CartesianFrame(
    val scale: CartesianScale,
    val layer: ChartLayer,
    val obstacles: List<ChartObstacle>,
)

internal fun cartesianFrame(
    context: ChartRenderContext,
    xRange: ChartRange,
    yRange: ChartRange,
    axes: AxisStyle,
    showXValues: Boolean = true,
): CartesianFrame = with(context.density) {
    val gap = 6.dp.toPx()
    val padding = 12.dp.toPx()
    val ticks = (0 until axes.tickCount).map { it.toDouble() / (axes.tickCount - 1) }
    val yTexts = ticks.map { context.textMeasurer.measure(axes.formatter(yRange.min * (1 - it) + yRange.max * it), context.labelStyle.textStyle) }
    val xTexts = ticks.map { context.textMeasurer.measure(axes.formatter(xRange.min * (1 - it) + xRange.max * it), context.labelStyle.textStyle) }
    val left = if (axes.visible) (yTexts.maxOf { it.size.width } + gap * 2).coerceAtLeast(xTexts.first().size.width / 2f) else padding
    val right = if (axes.visible && showXValues) max(padding, xTexts.last().size.width / 2f) else padding
    val bottom = if (axes.visible) xTexts.maxOf { it.size.height } + gap * 2 else padding
    val top = max(padding, if (context.labelStyle.visible) context.labelStyle.reservedTopSpace.toPx() else padding)
    val plot = ChartBounds(min(left, context.width * .4f), min(top, context.height * .65f),
        max(context.width - right, context.width * .6f), max(context.height - bottom, context.height * .7f))
    val scale = CartesianScale(xRange, yRange, plot)
    val textPositions = mutableListOf<Pair<androidx.compose.ui.text.TextLayoutResult, Offset>>()
    val obstacles = mutableListOf<ChartObstacle>()
    val boxes = mutableListOf<ChartBounds>()
    fun addText(text: androidx.compose.ui.text.TextLayoutResult, offset: Offset) {
        val bounds = ChartBounds(offset.x, offset.y, offset.x + text.size.width, offset.y + text.size.height)
        if (ChartBounds(0f, 0f, context.width, context.height).contains(bounds) && boxes.none { it.expanded(2f).intersects(bounds) }) {
            boxes += bounds
            obstacles += RectangleObstacle(bounds)
            textPositions += text to offset
        }
    }
    if (axes.visible) {
        ticks.forEachIndexed { index, fraction ->
            val y = plot.bottom - fraction.toFloat() * plot.height
            addText(yTexts[index], Offset(plot.left - gap - yTexts[index].size.width, y - yTexts[index].size.height / 2f))
            if (showXValues) {
                val x = plot.left + fraction.toFloat() * plot.width
                addText(xTexts[index], Offset(x - xTexts[index].size.width / 2f, plot.bottom + gap))
            }
        }
    }
    CartesianFrame(scale, ChartLayer {
        if (axes.visible) {
            ticks.forEach { fraction ->
                val y = plot.bottom - fraction.toFloat() * plot.height
                drawLine(axes.gridColor, Offset(plot.left, y), Offset(plot.right, y), 1.dp.toPx())
            }
            drawLine(axes.color, Offset(plot.left, plot.top), Offset(plot.left, plot.bottom), 1.dp.toPx())
            drawLine(axes.color, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), 1.dp.toPx())
            textPositions.forEach { (text, offset) -> drawText(text, topLeft = offset) }
        }
    }, obstacles)
}
