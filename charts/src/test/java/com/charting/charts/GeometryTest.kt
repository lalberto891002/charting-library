package com.charting.charts

import com.charting.charts.core.CartesianScale
import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.PolygonObstacle
import com.charting.charts.core.PolylineObstacle
import com.charting.charts.core.lineSegment
import com.charting.charts.core.pieFractions
import com.charting.charts.core.resolveSectionOwners
import com.charting.charts.model.ChartPoint
import com.charting.charts.model.ChartRange
import com.charting.charts.model.ChartSeries
import com.charting.charts.model.PieSlice
import org.junit.Assert.*
import org.junit.Test

class GeometryTest {
    @Test fun emptyAndConstantRangesHaveNonzeroSpan() {
        assertEquals(ChartRange(0.0, 1.0), ChartRange.enclosing(emptyList()))
        val range = ChartRange.enclosing(listOf(5.0, 5.0))
        assertTrue(range.min < 5.0 && range.max > 5.0)
    }

    @Test fun scaleSupportsNegativeValuesAndZero() {
        val scale = CartesianScale(ChartRange(0.0, 10.0), ChartRange(-5.0, 5.0), ChartBounds(0f, 0f, 100f, 100f))
        assertEquals(50f, scale.x(5.0), .001f)
        assertEquals(50f, scale.y(0.0), .001f)
        assertEquals(100f, scale.y(-5.0), .001f)
    }

    @Test fun lastSectionOwnsOverlappingEdges() {
        assertEquals(listOf(null, 0, 1, 1, 1, null), resolveSectionOwners(
            listOf("a", "b", "c", "d", "e", "f", "g"), listOf("b" to "e", "c" to "f")))
    }

    @Test fun disjointSectionsLeaveUnfilledEdges() {
        assertEquals(listOf(0, null, 1), resolveSectionOwners(listOf("a", "b", "c", "d"), listOf("a" to "b", "c" to "d")))
    }

    @Test fun unknownReversedAndZeroLengthSectionsAreRejected() {
        for (section in listOf("a" to "missing", "b" to "a", "a" to "a")) {
            assertThrows(IllegalArgumentException::class.java) { resolveSectionOwners(listOf("a", "b"), listOf(section)) }
        }
    }

    @Test fun emptyAndSinglePointSeriesHaveNoFillEdges() {
        assertTrue(resolveSectionOwners(emptyList(), emptyList()).isEmpty())
        assertTrue(resolveSectionOwners(listOf("a"), emptyList()).isEmpty())
    }

    @Test fun polygonDetectsInteriorEdgeAndContainingRectangles() {
        val polygon = PolygonObstacle(listOf(ChartPosition(0f, 0f), ChartPosition(100f, 0f), ChartPosition(50f, 100f)))
        assertTrue(polygon.intersects(ChartBounds(45f, 20f, 55f, 30f)))
        assertTrue(polygon.intersects(ChartBounds(-10f, -10f, 110f, 110f)))
        assertTrue(polygon.intersects(ChartBounds(20f, 45f, 30f, 55f)))
        assertFalse(polygon.intersects(ChartBounds(90f, 90f, 100f, 100f)))
    }

    @Test fun linesIncludeStrokeThicknessAndCrossingWithoutContainedEndpoints() {
        val obstacle = PolylineObstacle(listOf(ChartPosition(0f, 50f), ChartPosition(100f, 50f)), 10f)
        assertTrue(obstacle.intersects(ChartBounds(40f, 44f, 60f, 46f)))
        assertFalse(obstacle.intersects(ChartBounds(40f, 20f, 60f, 30f)))
        assertTrue(PolylineObstacle(listOf(ChartPosition(50f, 0f), ChartPosition(50f, 100f)), 1f)
            .intersects(ChartBounds(45f, 45f, 55f, 55f)))
    }

    @Test fun smoothGeometryPreservesEndpointsAndDoesNotOvershoot() {
        val start = ChartPosition(0f, 100f)
        val end = ChartPosition(100f, 0f)
        val points = lineSegment(start, end, true)
        assertEquals(start, points.first())
        assertEquals(end, points.last())
        assertTrue(points.all { it.x in 0f..100f && it.y in 0f..100f })
        assertTrue(points.zipWithNext().all { (a, b) -> a.x <= b.x && a.y >= b.y })
    }

    @Test fun pieHandlesEmptyZeroAndVeryLargeFiniteValues() {
        assertTrue(pieFractions(emptyList()).isEmpty())
        assertEquals(listOf(0f, 0f), pieFractions(listOf(0.0, 0.0)))
        assertEquals(listOf(.5f, .5f), pieFractions(listOf(Double.MAX_VALUE, Double.MAX_VALUE)))
        assertEquals(1f, pieFractions(listOf(1.0, 2.0, 3.0)).sum(), .0001f)
    }

    @Test fun stepSeriesPreservesRepeatedXValuesAndSectionEndpoints() {
        val points = listOf(
            ChartPoint("a", 0.0, 100.0), ChartPoint("b", 8.0, 100.0),
            ChartPoint("c", 8.0, 50.0, label = "50"), ChartPoint("d", 16.0, 50.0),
            ChartPoint("e", 16.0, 75.0), ChartPoint("f", 24.0, 75.0),
        )
        val series = ChartSeries("steps", points)
        assertEquals(points, series.points)
        assertEquals(1, series.points.count { it.label != null })
        assertEquals(listOf(null, null, 0, null, null),
            resolveSectionOwners(points.map { it.id }, listOf("c" to "d")))
        assertEquals(listOf(null, 0, null, null, null),
            resolveSectionOwners(points.map { it.id }, listOf("b" to "c")))
    }

    @Test fun verticalSegmentsStayVerticalForBothInterpolations() {
        val start = ChartPosition(8f, 100f)
        val end = ChartPosition(8f, 50f)
        for (smooth in listOf(false, true)) {
            val points = lineSegment(start, end, smooth)
            assertEquals(start, points.first())
            assertEquals(end, points.last())
            assertTrue(points.all { it.x == 8f && it.y in 50f..100f })
        }
    }

    @Test fun invalidModelsFailWithClearContracts() {
        assertThrows(IllegalArgumentException::class.java) { ChartPoint("a", Double.NaN, 1.0) }
        assertThrows(IllegalArgumentException::class.java) { PieSlice("a", -1.0) }
        assertThrows(IllegalArgumentException::class.java) { ChartRange(1.0, 1.0) }
        assertThrows(IllegalArgumentException::class.java) {
            ChartSeries("a", listOf(ChartPoint("a", 2.0, 1.0), ChartPoint("b", 1.0, 2.0)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChartSeries("a", listOf(ChartPoint("same", 1.0, 1.0), ChartPoint("same", 2.0, 2.0)))
        }
    }
}
