package com.charting.charts.labels

import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartObstacle
import com.charting.charts.core.ChartPosition

data class LabelCandidate(
    val id: String,
    val anchor: ChartPosition,
    val width: Float,
    val height: Float,
    val priority: Int = 0,
)

data class LabelPlacementRequest(
    val labels: List<LabelCandidate>,
    val bounds: ChartBounds,
    val obstacles: List<ChartObstacle>,
    val gap: Float,
    val levelSpacing: Float,
    val maxLevels: Int,
)

/** Omit an ID to hide its visual label. Its value remains in chart accessibility semantics. */
fun interface LabelPlacementStrategy {
    fun place(request: LabelPlacementRequest): Map<String, ChartBounds>
}

/** Keeps X fixed, attempts higher levels and hides lower-priority labels when space runs out. */
object StaggeredLabelPlacement : LabelPlacementStrategy {
    override fun place(request: LabelPlacementRequest): Map<String, ChartBounds> {
        require(request.gap >= 0f && request.levelSpacing >= 0f && request.maxLevels > 0)
        val placed = linkedMapOf<String, ChartBounds>()
        request.labels.sortedWith(compareByDescending<LabelCandidate> { it.priority }.thenBy { it.id }).forEach { label ->
            for (level in 0 until request.maxLevels) {
                val bottom = label.anchor.y - request.gap - level * (label.height + request.levelSpacing)
                val box = ChartBounds(label.anchor.x - label.width / 2f, bottom - label.height,
                    label.anchor.x + label.width / 2f, bottom)
                val safeBox = box.expanded(request.gap)
                if (request.bounds.contains(box) && placed.values.none { it.intersects(safeBox) } &&
                    request.obstacles.none { it.intersects(safeBox) }) {
                    placed[label.id] = box
                    break
                }
            }
        }
        return placed
    }
}
