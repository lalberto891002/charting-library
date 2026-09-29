package com.charting.charts.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.charting.charts.labels.LabelCandidate
import com.charting.charts.labels.LabelPlacementRequest
import com.charting.charts.labels.LabelPlacementStrategy
import com.charting.charts.labels.StaggeredLabelPlacement
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LocalChartTheme
import kotlin.math.roundToInt

data class ChartLabel(
    val id: String,
    val text: String,
    val anchor: ChartPosition,
    val priority: Int = 0,
)

fun interface ChartLayer {
    fun DrawScope.draw()
}

data class ChartScene(
    val layers: List<ChartLayer> = emptyList(),
    val obstacles: List<ChartObstacle> = emptyList(),
    val labels: List<ChartLabel> = emptyList(),
)

data class ChartRenderContext(
    val width: Float,
    val height: Float,
    val density: Density,
    val textMeasurer: TextMeasurer,
    val labelStyle: LabelStyle,
)

/** New chart types only need to provide layers, obstacles and label anchors. */
fun interface ChartRenderer {
    fun createScene(context: ChartRenderContext): ChartScene
}

/**
 * Shared measurement, collision resolution and accessibility host.
 * Labels are revalidated at every geometry frame; they snap/hide instead of travelling through shapes.
 * Custom label content must draw within its measured bounds for collision guarantees to hold.
 */
@Composable
fun ChartSurface(
    renderer: ChartRenderer,
    contentDescription: String,
    modifier: Modifier = Modifier,
    labelStyle: LabelStyle = LocalChartTheme.current.labels,
    labelPlacement: LabelPlacementStrategy = StaggeredLabelPlacement,
    labelContent: (@Composable (ChartLabel) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    SubcomposeLayout(modifier.defaultMinSize(minWidth = 240.dp, minHeight = 240.dp)
        .clipToBounds().semantics { this.contentDescription = contentDescription }) { constraints ->
        val width = constraints.constrainWidth(if (constraints.hasBoundedWidth) constraints.maxWidth else 360.dp.roundToPx())
        val height = constraints.constrainHeight(if (constraints.hasBoundedHeight) constraints.maxHeight else 280.dp.roundToPx())
        val context = ChartRenderContext(width.toFloat(), height.toFloat(), this, textMeasurer, labelStyle)
        val scene = renderer.createScene(context)
        require(scene.labels.map { it.id }.distinct().size == scene.labels.size) { "Label IDs must be unique" }
        val labels = if (labelStyle.visible) scene.labels else emptyList()
        val measured = labels.associate { label ->
            label.id to subcompose("label:${label.id}") {
                Box(Modifier.padding(2.dp).clearAndSetSemantics { }) {
                    if (labelContent == null) BasicText(label.text, style = labelStyle.textStyle)
                    else labelContent(label)
                }
            }.single().measure(Constraints(maxWidth = width, maxHeight = height))
        }
        val positions = labelPlacement.place(LabelPlacementRequest(
            labels.map { label ->
                val placeable = measured.getValue(label.id)
                LabelCandidate(label.id, label.anchor, placeable.width.toFloat(), placeable.height.toFloat(), label.priority)
            },
            ChartBounds(0f, 0f, width.toFloat(), height.toFloat()), scene.obstacles,
            labelStyle.gap.toPx() + 1f, labelStyle.levelSpacing.toPx(), labelStyle.maxLevels,
        ))
        val canvas = subcompose("canvas") {
            Canvas(Modifier) { scene.layers.forEach { layer -> with(layer) { draw() } } }
        }.single().measure(Constraints.fixed(width, height))
        layout(width, height) {
            canvas.place(0, 0)
            positions.forEach { (id, bounds) ->
                measured[id]?.place(bounds.left.roundToInt(), bounds.top.roundToInt())
            }
        }
    }
}
