package com.charting.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.charting.charts.animation.NoChartAnimation
import com.charting.charts.core.ChartBounds
import com.charting.charts.core.ChartLabel
import com.charting.charts.core.ChartPosition
import com.charting.charts.core.ChartRenderer
import com.charting.charts.core.ChartScene
import com.charting.charts.core.ChartSurface
import com.charting.charts.labels.LabelPlacementRequest
import com.charting.charts.labels.LabelPlacementStrategy
import com.charting.charts.labels.StaggeredLabelPlacement
import com.charting.charts.model.BarEntry
import com.charting.charts.model.ChartPoint
import com.charting.charts.model.ChartSeries
import com.charting.charts.model.PieSlice
import com.charting.charts.style.AxisStyle
import com.charting.charts.style.FillBaseline
import com.charting.charts.style.FillPaint
import com.charting.charts.style.FillSection
import com.charting.charts.style.LabelStyle
import com.charting.charts.style.LineChartStyle
import com.charting.charts.style.LineFillStyle
import com.charting.charts.style.PointStyle
import com.charting.charts.ui.BarChart
import com.charting.charts.ui.LineChart
import com.charting.charts.ui.PieChart
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class ChartComposeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blueStepsFillOnlySelectedPlateauAndShowOneSafeLabel() {
        val captured = AtomicReference<Pair<LabelPlacementRequest, Map<String, ChartBounds>>>()
        val verticalOnly = mutableStateOf(false)
        val placement = LabelPlacementStrategy { request ->
            StaggeredLabelPlacement.place(request).also { captured.set(request to it) }
        }
        compose.setContent {
            LineChart(
                series = listOf(ChartSeries("steps", listOf(
                    ChartPoint("start", 0.0, 100.0),
                    ChartPoint("drop-top", 8.0, 100.0),
                    ChartPoint("drop-bottom", 8.0, 50.0, label = "50"),
                    ChartPoint("rise-bottom", 16.0, 50.0),
                    ChartPoint("rise-top", 16.0, 75.0),
                    ChartPoint("end", 24.0, 75.0),
                ))),
                modifier = Modifier.size(360.dp).background(Color.White).testTag("steps"),
                style = LineChartStyle(
                    color = Color.Blue,
                    dots = PointStyle(color = Color.Blue),
                    fill = LineFillStyle(
                        sections = listOf(if (verticalOnly.value)
                            FillSection("drop-top", "drop-bottom", FillPaint(Color.Blue, .25f))
                        else FillSection("drop-bottom", "rise-bottom", FillPaint(Color.Blue, .25f))),
                        baseline = FillBaseline.Zero,
                    ),
                ),
                axes = AxisStyle(visible = false),
                labelPlacement = placement,
                animation = NoChartAnimation,
            )
        }
        val pixels = compose.onNodeWithTag("steps").captureToImage().toPixelMap()
        val filled = pixels[pixels.width / 2, pixels.height * 4 / 5]
        assertEquals(.75f, filled.red, .03f)
        assertEquals(.75f, filled.green, .03f)
        assertEquals(1f, filled.blue, .03f)
        for (x in listOf(pixels.width / 5, pixels.width * 4 / 5)) {
            assertEquals(Color.White, pixels[x, pixels.height * 4 / 5])
        }
        val (request, placed) = captured.get()
        assertEquals(1, request.labels.size)
        assertEquals(1, placed.size)
        assertSafe(request, placed)
        val anchor = request.labels.single().anchor
        val verticalLine = pixels[anchor.x.toInt(), (anchor.y - pixels.height * .15f).toInt()]
        assertEquals(0f, verticalLine.red, .03f)
        assertEquals(0f, verticalLine.green, .03f)
        assertEquals(1f, verticalLine.blue, .03f)
        val dot = pixels[anchor.x.toInt(), anchor.y.toInt()]
        assertEquals(Color.Blue, dot)

        // A section whose endpoints share X has no area; do not invent a band of fill.
        compose.runOnIdle { verticalOnly.value = true }
        val verticalPixels = compose.onNodeWithTag("steps").captureToImage().toPixelMap()
        assertEquals(Color.White, verticalPixels[verticalPixels.width / 2, verticalPixels.height * 4 / 5])
    }

    @Test fun overlappingTransparentSectionsDoNotBlend() {
        compose.setContent {
            LineChart(listOf(ChartSeries("s", listOf("a", "b", "c", "d").mapIndexed { index, id ->
                ChartPoint(id, index.toDouble(), 10.0)
            })), Modifier.size(300.dp).background(Color.White).testTag("chart"),
                style = LineChartStyle(fill = LineFillStyle(sections = listOf(
                    FillSection("a", "d", FillPaint(Color.Red, .5f)),
                    FillSection("b", "c", FillPaint(Color.Blue, .5f)),
                ))), axes = AxisStyle(visible = false), labelStyle = LabelStyle(visible = false), animation = NoChartAnimation)
        }
        val pixels = compose.onNodeWithTag("chart").captureToImage().toPixelMap()
        val overlap = pixels[pixels.width / 2, pixels.height * 3 / 4]
        assertEquals(.5f, overlap.red, .03f)
        assertEquals(.5f, overlap.green, .03f)
        assertEquals(1f, overlap.blue, .03f)
        val first = pixels[pixels.width / 5, pixels.height * 3 / 4]
        assertEquals(1f, first.red, .03f)
        assertEquals(.5f, first.blue, .03f)
    }

    @Test fun hiddenLabelsKeepValuesAccessibleAndEmptyChartsRender() {
        compose.setContent {
            Column {
                LineChart(listOf(ChartSeries("s", listOf(ChartPoint("p", 1.0, 2.0, "Important value")))),
                    Modifier.size(240.dp), labelStyle = LabelStyle(visible = false), animation = NoChartAnimation)
                PieChart(emptyList(), Modifier.size(240.dp), animation = NoChartAnimation)
                BarChart(emptyList(), Modifier.size(240.dp), animation = NoChartAnimation)
            }
        }
        compose.onNodeWithContentDescription("Important value", substring = true).assertExists()
        compose.onNodeWithContentDescription("Pie chart. No data.").assertExists()
        compose.onNodeWithContentDescription("Bar chart. No data.").assertExists()
    }

    @Test fun customRendererAndLargeComposableLabelsUseSharedPlacement() {
        val captured = AtomicReference<Pair<LabelPlacementRequest, Map<String, ChartBounds>>>()
        val placement = LabelPlacementStrategy { request ->
            StaggeredLabelPlacement.place(request).also { captured.set(request to it) }
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ChartSurface(ChartRenderer { context -> ChartScene(labels = listOf(
                    ChartLabel("a", "First", ChartPosition(context.width / 2, context.height * .8f)),
                    ChartLabel("b", "Second", ChartPosition(context.width / 2, context.height * .8f)),
                )) }, "Custom chart", Modifier.size(320.dp), labelPlacement = placement,
                    labelContent = { BasicText(it.text) })
            }
        }
        compose.waitForIdle()
        val (request, placed) = captured.get()
        assertEquals(2, placed.size)
        assertSafe(request, placed)
        assertNotEquals(placed.getValue("a").top, placed.getValue("b").top)
    }

    @Test fun animatedUpdatesRevalidateLabelsAtEverySampledFrame() {
        compose.mainClock.autoAdvance = false
        val high = mutableStateOf(false)
        val captured = AtomicReference<Pair<LabelPlacementRequest, Map<String, ChartBounds>>>()
        val placement = LabelPlacementStrategy { request ->
            StaggeredLabelPlacement.place(request).also { captured.set(request to it) }
        }
        compose.setContent {
            LineChart(listOf(ChartSeries("s", (0..4).map {
                ChartPoint("p$it", it.toDouble(), if (high.value) 8.0 - it else 2.0 + it, "Label $it")
            })), Modifier.size(360.dp), style = LineChartStyle(fill = LineFillStyle(full = FillPaint(Color.Cyan, .3f))),
                labelPlacement = placement)
        }
        repeat(5) {
            compose.mainClock.advanceTimeBy(128)
            compose.waitForIdle()
            captured.get().let { (request, placed) -> assertSafe(request, placed) }
        }
        compose.runOnIdle { high.value = true }
        repeat(6) {
            compose.mainClock.advanceTimeBy(112)
            compose.waitForIdle()
            captured.get().let { (request, placed) -> assertSafe(request, placed) }
        }
    }

    @Test fun pieAndNegativeBarsRenderWithCustomColors() {
        compose.setContent {
            Column {
                PieChart(listOf(PieSlice("a", 4.0, fill = FillPaint(Color.Magenta)), PieSlice("b", 0.0)),
                    Modifier.size(240.dp), animation = NoChartAnimation)
                BarChart(listOf(BarEntry("negative", -5.0)), Modifier.size(240.dp), animation = NoChartAnimation)
            }
        }
        compose.onNodeWithContentDescription("a: 4.0", substring = true).assertExists()
        compose.onNodeWithContentDescription("negative: -5.0", substring = true).assertExists()
    }

    private fun assertSafe(request: LabelPlacementRequest, placed: Map<String, ChartBounds>) {
        val boxes = placed.values.toList()
        boxes.forEachIndexed { index, box ->
            assertTrue(request.bounds.contains(box))
            assertTrue(request.obstacles.none { it.intersects(box.expanded(request.gap)) })
            assertTrue(boxes.drop(index + 1).none { it.intersects(box.expanded(request.gap)) })
        }
    }
}
