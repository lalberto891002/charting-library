package com.charting.charts.core

import com.charting.charts.model.ChartRange
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

data class ChartPosition(val x: Float, val y: Float)

data class ChartBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top
    fun contains(p: ChartPosition) = p.x in left..right && p.y in top..bottom
    fun contains(b: ChartBounds) = b.left >= left && b.right <= right && b.top >= top && b.bottom <= bottom
    fun intersects(b: ChartBounds) = left <= b.right && right >= b.left && top <= b.bottom && bottom >= b.top
    fun expanded(padding: Float) = ChartBounds(left - padding, top - padding, right + padding, bottom + padding)
    val corners get() = listOf(ChartPosition(left, top), ChartPosition(right, top), ChartPosition(right, bottom), ChartPosition(left, bottom))
}

class CartesianScale(val xRange: ChartRange, val yRange: ChartRange, val plot: ChartBounds) {
    fun x(value: Double) = plot.left + ((value - xRange.min) / (xRange.max - xRange.min)).toFloat() * plot.width
    fun y(value: Double) = plot.bottom - ((value - yRange.min) / (yRange.max - yRange.min)).toFloat() * plot.height
}

/** Extension point for custom shapes that labels must avoid. Coordinates are pixels. */
fun interface ChartObstacle {
    fun intersects(bounds: ChartBounds): Boolean
}

data class RectangleObstacle(val bounds: ChartBounds) : ChartObstacle {
    override fun intersects(bounds: ChartBounds) = this.bounds.intersects(bounds)
}

data class PolylineObstacle(val points: List<ChartPosition>, val strokeWidth: Float) : ChartObstacle {
    override fun intersects(bounds: ChartBounds): Boolean {
        val padded = bounds.expanded(strokeWidth / 2f)
        return points.any(padded::contains) || points.zipWithNext().any { (a, b) -> segmentIntersectsBox(a, b, padded) }
    }
}

data class PolygonObstacle(val points: List<ChartPosition>) : ChartObstacle {
    override fun intersects(bounds: ChartBounds): Boolean {
        if (points.isEmpty()) return false
        return points.any(bounds::contains) || bounds.corners.any { pointInPolygon(it, points) } ||
            (points + points.first()).zipWithNext().any { (a, b) -> segmentIntersectsBox(a, b, bounds) }
    }
}

private fun segmentIntersectsBox(a: ChartPosition, b: ChartPosition, box: ChartBounds): Boolean {
    // Liang-Barsky clipping handles vertical, horizontal and zero-length segments.
    var low = 0f
    var high = 1f
    val dx = b.x - a.x
    val dy = b.y - a.y
    val p = floatArrayOf(-dx, dx, -dy, dy)
    val q = floatArrayOf(a.x - box.left, box.right - a.x, a.y - box.top, box.bottom - a.y)
    for (i in p.indices) {
        if (abs(p[i]) < 0.00001f) {
            if (q[i] < 0f) return false
        } else {
            val t = q[i] / p[i]
            if (p[i] < 0f) low = max(low, t) else high = min(high, t)
            if (low > high) return false
        }
    }
    return true
}

private fun pointInPolygon(point: ChartPosition, polygon: List<ChartPosition>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > point.y) != (b.y > point.y) &&
            point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) inside = !inside
        j = i
    }
    return inside
}

/** A bounded, non-overshooting smooth curve sampled once for both drawing and collision detection. */
fun lineSegment(a: ChartPosition, b: ChartPosition, smooth: Boolean): List<ChartPosition> {
    if (!smooth) return listOf(a, b)
    val steps = ceil(max(abs(b.x - a.x), abs(b.y - a.y)) / 2f).toInt().coerceIn(8, 512)
    return (0..steps).map { step ->
        val t = step.toFloat() / steps
        val eased = t * t * (3f - 2f * t)
        ChartPosition(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * eased)
    }
}

/** Each edge has one owner, so overlapping sections never blend accidentally. */
fun resolveSectionOwners(pointIds: List<String>, sections: List<Pair<String, String>>): List<Int?> {
    val indices = pointIds.withIndex().associate { it.value to it.index }
    val owners = MutableList<Int?>(max(0, pointIds.size - 1)) { null }
    sections.forEachIndexed { section, (startId, endId) ->
        val start = requireNotNull(indices[startId]) { "Unknown fill start point: $startId" }
        val end = requireNotNull(indices[endId]) { "Unknown fill end point: $endId" }
        require(start < end) { "Fill sections require start before end" }
        for (edge in start until end) owners[edge] = section
    }
    return owners
}

/** Normalize before summing to avoid overflow with large finite slice values. */
fun pieFractions(values: List<Double>): List<Float> {
    require(values.all { it.isFinite() && it >= 0.0 })
    val maximum = values.maxOrNull() ?: return emptyList()
    if (maximum == 0.0) return values.map { 0f }
    val normalized = values.map { it / maximum }
    val total = normalized.sum()
    return normalized.map { (it / total).toFloat() }
}
