package com.charting.charting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.charting.charts.animation.DefaultChartAnimation
import com.charting.charts.animation.NoChartAnimation
import com.charting.charts.model.BarEntry
import com.charting.charts.model.ChartPoint
import com.charting.charts.model.ChartSeries
import com.charting.charts.model.PieSlice
import com.charting.charts.style.AxisStyle
import com.charting.charts.style.BarStyle
import com.charting.charts.style.ChartTheme
import com.charting.charts.style.ChartThemeData
import com.charting.charts.style.FillBaseline
import com.charting.charts.style.FillPaint
import com.charting.charts.style.FillSection
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LineChartStyle
import com.charting.charts.style.LineFillStyle
import com.charting.charts.style.LineInterpolation
import com.charting.charts.style.PieStyle
import com.charting.charts.style.PointStyle
import com.charting.charts.ui.BarChart
import com.charting.charts.ui.LineChart
import com.charting.charts.ui.PieChart

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { ChartGallery() }
            }
        }
    }
}

@Composable
private fun ChartGallery() {
    var revision by remember { mutableIntStateOf(0) }
    var animate by remember { mutableStateOf(true) }
    var smooth by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var sectional by remember { mutableStateOf(true) }
    val blue = Color(0xFF3B82F6)
    val green = Color(0xFF10B981)
    val orange = Color(0xFFF59E0B)
    val colors = MaterialTheme.colorScheme
    val theme = ChartThemeData(
        labels = LabelStyle(textStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurface), reservedTopSpace = 90.dp),
        axes = AxisStyle(color = colors.outline, gridColor = colors.outlineVariant),
        palette = listOf(blue, green, orange),
    )
    val animation = if (animate) DefaultChartAnimation else NoChartAnimation
    val labels = theme.labels.copy(visible = showLabels)
    val stepPoints = remember {
        listOf(
            ChartPoint("start", 0.0, 100.0),
            ChartPoint("drop-top", 8.0, 100.0),
            ChartPoint("drop-bottom", 8.0, 50.0, label = "50"),
            ChartPoint("rise-bottom", 16.0, 50.0),
            ChartPoint("rise-top", 16.0, 75.0),
            ChartPoint("end", 24.0, 75.0),
        )
    }
    val points = remember(revision) {
        listOf(3.0, 5.0, 4.8, 5.1, 7.0, 4.0, 6.0, 5.7).mapIndexed { index, value ->
            val y = value + if (revision % 2 == 0) 0.0 else (index % 3 - 1) * 1.5
            ChartPoint("p$index", index.toDouble(), y, "P$index: $y", labelPriority = if (index == 4) 10 else 0)
        }
    }
    val lineStyle = LineChartStyle(
        color = blue,
        dots = PointStyle(blue, 4.dp),
        interpolation = if (smooth) LineInterpolation.Smooth else LineInterpolation.Linear,
        fill = LineFillStyle(
            full = if (sectional) null else FillPaint(Brush.verticalGradient(listOf(blue.copy(alpha = .5f), blue.copy(alpha = .03f)))),
            sections = if (sectional) listOf(
                FillSection("p1", "p5", FillPaint(green, .25f)),
                FillSection("p3", "p6", FillPaint(Brush.verticalGradient(listOf(orange.copy(alpha = .55f), orange.copy(alpha = .08f))))),
            ) else emptyList(),
            baseline = FillBaseline.Zero,
        ),
    )
    ChartTheme(theme) {
        Column(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Compose Charts", style = MaterialTheme.typography.headlineMedium)
            Text("Styles, sectional fills, safe labels and extensible animations.")
            DemoCard("Blue step chart", "Blue points and line: (0,100), (8,100), (8,50), (16,50), (16,75), (24,75). Only X=8–16 is filled down to zero, with one label at (8,50).") {
                LineChart(
                    series = listOf(ChartSeries("steps", stepPoints, "Steps")),
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    style = LineChartStyle(
                        color = blue,
                        dots = PointStyle(color = blue, radius = 4.dp),
                        interpolation = LineInterpolation.Linear,
                        fill = LineFillStyle(
                            sections = listOf(FillSection("drop-bottom", "rise-bottom", FillPaint(blue, .25f))),
                            baseline = FillBaseline.Zero,
                        ),
                    ),
                    axes = theme.axes.copy(tickCount = 4),
                    labelStyle = labels,
                    animation = animation,
                )
            }
            Toggle("Animate changes", animate) { animate = it }
            Toggle("Smooth lines", smooth) { smooth = it }
            Toggle("Show collision-aware labels", showLabels) { showLabels = it }
            Toggle("Sectional fills (last section wins)", sectional) { sectional = it }
            Button(onClick = { revision++ }) { Text("Update data") }
            DemoCard("Line chart", "Green: P1–P5. Orange: P3–P6. Orange owns the overlap. Labels move upward or hide when no safe space remains.") {
                LineChart(listOf(ChartSeries("primary", points, "Revenue", lineStyle), ChartSeries("comparison",
                    points.map { it.copy(id = "comparison-${it.id}", y = it.y * .5, label = null) }, "Comparison",
                    LineChartStyle(color = green, dots = PointStyle(visible = false), dash = listOf(6.dp, 4.dp)))),
                    Modifier.fillMaxWidth().height(360.dp), labelStyle = labels, animation = animation)
            }
            DemoCard("Bar chart", "Each bar has its own fill. Negative values extend below zero.") {
                BarChart(listOf(
                    BarEntry("north", if (revision % 2 == 0) 8.0 else 5.0, "North", BarStyle(FillPaint(blue))),
                    BarEntry("south", -3.0, "South", BarStyle(FillPaint(orange))),
                    BarEntry("west", 6.0, "West", BarStyle(FillPaint(green))),
                ), Modifier.fillMaxWidth().height(300.dp), labelStyle = labels, animation = animation)
            }
            DemoCard("Pie / donut chart", "Stable IDs animate data updates; each slice can override its fill.") {
                PieChart(listOf(PieSlice("mobile", if (revision % 2 == 0) 50.0 else 30.0, "Mobile"),
                    PieSlice("web", 30.0, "Web"), PieSlice("desktop", 20.0, "Desktop")),
                    Modifier.fillMaxWidth().height(340.dp), style = PieStyle(holeFraction = .35f), labelStyle = labels, animation = animation)
            }
        }
    }
}

@Composable
private fun Toggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f))
        Switch(checked, onCheckedChange)
    }
}

@Composable
private fun DemoCard(title: String, description: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}
