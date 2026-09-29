package com.charting.charts.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FillPaint(val brush: Brush, val alpha: Float = 1f) {
    constructor(color: Color, alpha: Float = 1f) : this(androidx.compose.ui.graphics.SolidColor(color), alpha)
    init { require(alpha.isFinite() && alpha in 0f..1f) }
}

/** The area between the line and this baseline is filled, including for negative values. */
sealed interface FillBaseline {
    data object Bottom : FillBaseline
    data object Zero : FillBaseline
    data class Value(val y: Double) : FillBaseline {
        init { require(y.isFinite()) }
    }
}

data class FillSection(val startPointId: String, val endPointId: String, val paint: FillPaint)

/** Sections override [full] in declaration order, without stacking transparent paints. */
data class LineFillStyle(
    val full: FillPaint? = null,
    val sections: List<FillSection> = emptyList(),
    val baseline: FillBaseline = FillBaseline.Bottom,
)

data class PointStyle(
    val color: Color = Color(0xFF2563EB),
    val radius: Dp = 4.dp,
    val visible: Boolean = true,
) {
    init { require(radius.value.isFinite() && radius.value >= 0f) }
}

enum class LineInterpolation { Linear, Smooth }

data class LineChartStyle(
    val color: Color = Color(0xFF2563EB),
    val width: Dp = 2.dp,
    val dots: PointStyle = PointStyle(),
    val fill: LineFillStyle = LineFillStyle(),
    val interpolation: LineInterpolation = LineInterpolation.Linear,
    val dash: List<Dp> = emptyList(),
) {
    init {
        require(width.value.isFinite() && width.value > 0f)
        require(dash.isEmpty() || (dash.size >= 2 && dash.size % 2 == 0 && dash.all { it.value.isFinite() && it.value > 0f }))
    }
}

data class BarStyle(val fill: FillPaint = FillPaint(Color(0xFF2563EB)))

data class PieStyle(
    val startAngle: Float = -90f,
    val holeFraction: Float = 0f,
) {
    init { require(startAngle.isFinite() && holeFraction.isFinite() && holeFraction in 0f..0.9f) }
}

data class LabelStyle(
    val textStyle: TextStyle = TextStyle(color = Color(0xFF172554), fontSize = 12.sp),
    val gap: Dp = 6.dp,
    val levelSpacing: Dp = 6.dp,
    val maxLevels: Int = 12,
    val reservedTopSpace: Dp = 64.dp,
    val visible: Boolean = true,
) {
    init {
        require(maxLevels in 1..1000)
        require(listOf(gap, levelSpacing, reservedTopSpace).all { it.value.isFinite() && it.value >= 0f })
    }
}

data class AxisStyle(
    val visible: Boolean = true,
    val color: Color = Color(0xFF64748B),
    val gridColor: Color = Color(0xFFE2E8F0),
    val tickCount: Int = 5,
    val formatter: (Double) -> String = { value -> java.lang.String.format(java.util.Locale.ROOT, "%.1f", value) },
) {
    init { require(tickCount in 2..20) }
}

data class ChartThemeData(
    val line: LineChartStyle = LineChartStyle(),
    val bar: BarStyle = BarStyle(),
    val pie: PieStyle = PieStyle(),
    val labels: LabelStyle = LabelStyle(),
    val axes: AxisStyle = AxisStyle(),
    val palette: List<Color> = listOf(Color(0xFF2563EB), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444)),
) {
    init { require(palette.isNotEmpty()) }
}

val LocalChartTheme = staticCompositionLocalOf { ChartThemeData() }

@Composable
fun ChartTheme(theme: ChartThemeData, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalChartTheme provides theme, content = content)
}
