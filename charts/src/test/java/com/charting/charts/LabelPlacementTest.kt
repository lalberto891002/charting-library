package com.charting.charts

import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.PolygonObstacle
import com.charting.charts.core.PolylineObstacle
import com.charting.charts.core.RectangleObstacle
import com.charting.charts.labels.LabelCandidate
import com.charting.charts.labels.LabelPlacementRequest
import com.charting.charts.labels.StaggeredLabelPlacement
import org.junit.Assert.*
import org.junit.Test

class LabelPlacementTest {
    private val bounds = ChartBounds(0f, 0f, 300f, 300f)
    private fun label(id: String, x: Float = 100f, y: Float = 200f, priority: Int = 0) =
        LabelCandidate(id, ChartPosition(x, y), 60f, 20f, priority)

    @Test fun collidingLabelsMoveToDifferentHeightsWithoutChangingX() {
        val request = LabelPlacementRequest(listOf(label("a"), label("b", x = 110f)), bounds, emptyList(), 4f, 5f, 8)
        val placed = StaggeredLabelPlacement.place(request)
        assertEquals(2, placed.size)
        assertNotEquals(placed.getValue("a").top, placed.getValue("b").top)
        assertEquals(80f, placed.getValue("b").left, .001f)
        assertFalse(placed.getValue("a").intersects(placed.getValue("b")))
    }

    @Test fun labelsAvoidFillsAndLinesFromOtherSeries() {
        val fill = PolygonObstacle(listOf(ChartPosition(0f, 140f), ChartPosition(300f, 140f), ChartPosition(300f, 300f), ChartPosition(0f, 300f)))
        val line = PolylineObstacle(listOf(ChartPosition(0f, 100f), ChartPosition(300f, 100f)), 6f)
        val request = LabelPlacementRequest(listOf(label("a"), label("b", 110f)), bounds, listOf(fill, line), 4f, 5f, 12)
        val placed = StaggeredLabelPlacement.place(request)
        assertEquals(2, placed.size)
        placed.values.forEach { box -> assertTrue(request.obstacles.none { it.intersects(box.expanded(request.gap)) }) }
    }

    @Test fun higherPriorityGetsLimitedSpace() {
        val request = LabelPlacementRequest(listOf(label("a"), label("z", priority = 10)), bounds, emptyList(), 4f, 5f, 1)
        assertEquals(setOf("z"), StaggeredLabelPlacement.place(request).keys)
    }

    @Test fun occupiedOrOutOfBoundsLabelsAreHidden() {
        val request = LabelPlacementRequest(listOf(label("a"), label("b", x = 0f)), bounds,
            listOf(RectangleObstacle(bounds)), 4f, 5f, 12)
        assertTrue(StaggeredLabelPlacement.place(request).isEmpty())
    }

    @Test fun placementIsDeterministicAcrossInputOrder() {
        val request = LabelPlacementRequest(listOf(label("a"), label("b")), bounds, emptyList(), 4f, 5f, 8)
        assertEquals(StaggeredLabelPlacement.place(request), StaggeredLabelPlacement.place(request.copy(labels = request.labels.reversed())))
    }

    @Test fun largeFontMeasurementsAndDenseLabelsRemainSafe() {
        val candidates = (0..20).map { label("p$it", x = 80f + it * 5).copy(width = 120f, height = 48f) }
        val request = LabelPlacementRequest(candidates, bounds, emptyList(), 6f, 8f, 12)
        val placed = StaggeredLabelPlacement.place(request).values.toList()
        assertTrue(placed.isNotEmpty() && placed.size < candidates.size)
        placed.forEachIndexed { index, box ->
            assertTrue(bounds.contains(box))
            assertTrue(placed.drop(index + 1).none { it.intersects(box.expanded(6f)) })
        }
    }
}
