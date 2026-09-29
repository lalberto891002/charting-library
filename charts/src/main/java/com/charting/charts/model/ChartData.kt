package com.charting.charts.model

import com.charting.charts.style.BarStyle
import com.charting.charts.style.FillPaint
import com.charting.charts.style.LineChartStyle
import com.charting.charts.style.PointStyle

/** IDs must remain stable across updates. Submit new lists instead of mutating existing data. */
data class ChartPoint(
    val id: String,
    val x: Double,
    val y: Double,
    val label: String? = null,
    val style: PointStyle? = null,
    val labelPriority: Int = 0,
) {
    init { require(id.isNotBlank() && x.isFinite() && y.isFinite()) }
}

/** Points follow non-decreasing X order; repeated X values create vertical segments in list order. */
data class ChartSeries(
    val id: String,
    val points: List<ChartPoint>,
    val name: String = id,
    val style: LineChartStyle? = null,
) {
    init {
        require(id.isNotBlank())
        require(points.map { it.id }.distinct().size == points.size) { "Point IDs must be unique within a series" }
        require(points.zipWithNext().all { (a, b) -> a.x <= b.x }) { "X values must be non-decreasing" }
    }
}

data class PieSlice(
    val id: String,
    val value: Double,
    val label: String = id,
    val fill: FillPaint? = null,
) {
    init { require(id.isNotBlank() && value.isFinite() && value >= 0.0) { "Pie values must be finite and non-negative" } }
}

data class BarEntry(
    val id: String,
    val value: Double,
    val label: String = id,
    val style: BarStyle? = null,
    val labelPriority: Int = 0,
) {
    init { require(id.isNotBlank() && value.isFinite()) }
}

data class ChartRange(val min: Double, val max: Double) {
    init { require(min.isFinite() && max.isFinite() && min < max && (max - min).isFinite()) }

    companion object {
        fun enclosing(values: List<Double>, includeZero: Boolean = false): ChartRange {
            require(values.all { it.isFinite() })
            val candidates = if (includeZero) values + 0.0 else values
            val low = candidates.minOrNull() ?: 0.0
            val high = candidates.maxOrNull() ?: 1.0
            if (low < high) return ChartRange(low, high)
            val padding = maxOf(kotlin.math.abs(low) * 0.05, 1.0)
            return ChartRange(low - padding, high + padding)
        }
    }
}
