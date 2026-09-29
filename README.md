# Compose Charts

An Android-first Jetpack Compose chart library with extensible rendering, styles,
animation and collision-aware labels. Minimum Android API: **24**.

## Modules

- [`:charts`](charts/README.md): reusable library, with no Material dependency;
  see its guide for installation, public API contracts and extension points.
- `:app`: interactive Material 3 gallery. Launch it to switch between smooth/straight
  lines, full/sectional fills, animated/static updates and visible/hidden labels.
  Its first example draws blue steps through `(0,100)`, `(8,100)`, `(8,50)`,
  `(16,50)`, `(16,75)` and `(24,75)`. Only the plateau from `(8,50)` to `(16,50)`
  is filled blue down to zero, and only `(8,50)` has a point label.

The initial API is version `0.1.0`; it is not yet a stable compatibility commitment.
See [CONTRIBUTING.md](CONTRIBUTING.md) for development checks, commit conventions
and the task-based import history.

## Build and test

Use JDK 21 (Android Studio's bundled JBR works) and the Android SDK configured in
`local.properties`. AGP 9 uses built-in Kotlin; do not add the legacy Kotlin Android plugin.
Install SDK platforms 36 and 37: the library compiles against API 36, while the demo
compiles against API 37 because AndroidX Core 1.19.1 requires it. The demo retains
target API 36, and both modules retain minimum API 24.

```shell
./gradlew :app:assembleDebug :charts:testDebugUnitTest
./gradlew :charts:connectedDebugAndroidTest  # requires an emulator or device
./gradlew :charts:lintDebug :app:lintDebug
./gradlew :charts:publishReleasePublicationToMavenLocal
```

The Maven-local coordinates are `com.charting:charts-compose:0.1.0`. No remote
repository publication, credentials or signing are configured.

Within this project, depend on `implementation(project(":charts"))` and enable
Compose in the consuming module. For a separate application, publish locally and
add `mavenLocal()` to its dependency repositories. Align its Compose dependencies
with the library's BOM or a compatible newer BOM.

## Line chart and sectional area fills

```kotlin
val points = listOf(
    ChartPoint("a", 0.0, 3.0, label = "Start"),
    ChartPoint("b", 1.0, 6.0, label = "Peak", labelPriority = 10),
    ChartPoint("c", 2.0, 4.0, label = "Middle"),
    ChartPoint("d", 3.0, 5.0, label = "End"),
)

LineChart(
    series = listOf(ChartSeries("revenue", points)),
    modifier = Modifier.fillMaxWidth().height(320.dp),
    style = LineChartStyle(
        color = Color.Blue,
        width = 3.dp,
        dots = PointStyle(color = Color.Blue, radius = 4.dp),
        interpolation = LineInterpolation.Smooth,
        fill = LineFillStyle(
            full = FillPaint(Color.Blue, alpha = 0.08f),
            sections = listOf(
                FillSection("a", "c", FillPaint(Color.Green, alpha = 0.25f)),
                FillSection("b", "d", FillPaint(Color.Magenta, alpha = 0.35f)),
            ),
            baseline = FillBaseline.Zero,
        ),
    ),
)
```

Imports are under `com.charting.charts.model`, `.style`, `.ui`, `.animation`,
`.labels` and `.core`; normal Compose types keep their AndroidX imports.

### Fill contracts

- A section selects two **stable point IDs within one series**, not array indexes.
- It follows all intermediate segments, including the same sampled smooth curve
  used to draw the line. It closes vertically from its endpoints to the baseline.
- `full = null` leaves unselected edges empty. A non-null `full` is the fallback
  fill for edges not covered by a section.
- Later sections **replace**, rather than blend with, earlier sections on shared
  edges. The full fallback is not drawn behind an overriding section either.
  Different series still composite in series order.
- Start must precede end in the point list. Unknown IDs, reversed endpoints and
  sections using the same endpoint ID throw `IllegalArgumentException`.
  Distinct endpoints can share X: a purely vertical section has zero fill area.
- Baselines: `Bottom`, `Zero`, or `Value(y)`. Out-of-range baselines are clipped to
  the plot. With negative data this is the area **between the curve and baseline**,
  which is not necessarily visually below the curve.
- `FillPaint` accepts a solid color or any Compose `Brush`, plus independent alpha.
  Gradient coordinates use the chart's pixel coordinate system, not a per-section
  coordinate system. Supply explicit brush endpoints when needed.
- Fills, lines and dots are drawn in that order; labels are drawn last.

## Bars, pies and themes

`BarChart` supports positive, negative and zero values, a configurable width
fraction, and per-entry `BarStyle`/`FillPaint`. `PieChart` supports non-negative
values, per-slice paint, a start angle and `PieStyle(holeFraction = 0.35f)` for donuts.
An all-zero pie is empty; zero-value slices have no visible wedge.

`ChartTheme(ChartThemeData(...))` supplies common labels, axes, a palette and chart
defaults through `LocalChartTheme`. The library does not assume a Material theme;
provide appropriate text, line and axis colors for dark backgrounds.

Style precedence is explicit element override → series override → chart parameter
→ theme/default. For lines, the theme palette supplies automatic series colors and
dot colors when neither a series nor a chart style is supplied. A point can override
its marker; a slice/bar can override its fill. Labels share the chart's `LabelStyle`
but their composable slot can vary its appearance by label ID. Use data-class `copy`
to derive presets without inheritance.

Cartesian charts have configurable axes, grid lines, tick count, number formatting
and optional explicit ranges. Automatic Y ranges include zero and animated values.
Empty inputs are valid, constant ranges are expanded, and single-point line series
can display a marker without a line. X coordinates must be non-decreasing in each
series. Repeated X values retain list order and create vertical segments, allowing
step charts without adding synthetic points. Numeric values must be finite and the
resulting axis span representable.
IDs must be unique within their scope. Submit new lists instead of mutating existing
data in place.

## Collision-aware labels

`StaggeredLabelPlacement` is the default `LabelPlacementStrategy`:

1. Measure actual text/composable content, including padding and font scale.
2. Sort by descending priority, then stable ID for deterministic results.
3. Keep the point's X anchor and try progressively higher positions.
4. Reject positions touching another label, any data line (including stroke width),
   visible marker, fill polygon, bar or pie wedge, or an axis tick label.
5. Hide a label if no candidate fits the chart bounds within `maxLevels`.

It considers **all series**, not only the series owning the label. Grid lines are
background decoration and are not obstacles. Dashed lines use their continuous
trajectory conservatively. Smooth lines and their fills use identical sampled
geometry for drawing and collision checks.

Configure `gap`, `levelSpacing`, `maxLevels`, `reservedTopSpace`, `visible` and
`textStyle` in `LabelStyle`. Reserved space is inside the supplied chart height;
bounded layouts cannot guarantee that every label will fit. Endpoints near the
left/right edge may hide because the default never shifts X. A custom strategy can
permit horizontal movement or different priority rules.

Line labels are opt-in through `ChartPoint.label`. Bar labels include category and
formatted value. Pie labels use the slice label. Use `labelContent` for custom
composable labels. Custom content must draw within its measured bounds: unmeasured
shadows, offsets or drawing outside the layout cannot be collision-checked.

During animation, layout is checked against the **current frame's** geometry. Labels
snap to a safe position or disappear; they do not animate through obstacles. This
prioritizes non-overlap over perfectly smooth label motion.

All data values, including visually hidden labels, remain in the chart's accessibility
description. Override `contentDescription` for a localized summary. The initial API
exposes one chart summary, not individually focusable/selectable data points.

## Animation

`GrowingChartAnimation(tween(600))` is the default. It animates Y values from zero,
interpolates X/Y updates by `(seriesId, pointId)`, and continues interrupted updates
from the last displayed frame. New IDs grow from zero; removed IDs disappear
immediately. Pie entrance also animates the sweep. Style changes are immediate.

Pass `NoChartAnimation` to disable animation, or a custom Compose finite animation
specification to `GrowingChartAnimation`. The default respects Compose's animation
duration scale. Fractions are clamped to avoid invalid negative pie values from
overshooting specifications.

Implement `ChartAnimationStrategy.values` to supply another state/transition model.
The returned map must contain every target ID with finite values; pie values must
remain non-negative. Keep strategy objects stable across recompositions.

## Extending the library

- **New chart:** implement `ChartRenderer.createScene` and host it in `ChartSurface`.
  `ChartRenderContext` provides pixel size, density, text measurement and label style.
- **New drawing/style:** capture your own style model in a renderer. Return ordered
  `ChartLayer` instances; each has a Compose `DrawScope`. Built-in styles remain small
  immutable value objects rather than a closed inheritance hierarchy.
- **New geometry:** implement `ChartObstacle.intersects` for labels to avoid it.
  Polylines, polygons and rectangles are included.
- **New label policy:** implement `LabelPlacementStrategy`. Omit an ID to hide it.
- **New animation:** implement `ChartAnimationStrategy`, or reuse the supplied
  implementation with a different animation specification.

Always return obstacles matching the geometry you draw. The host cannot infer
obstacles from arbitrary `DrawScope` commands. Custom strategies are responsible
for their own collision guarantees. `ChartSurface` uses physical data coordinates
and keeps increasing X left-to-right, including in RTL application layouts.

## Verification and initial scope

Local unit tests cover range/model contracts, overlapping sections, smooth paths,
polygon/line collisions, priorities, dense labels and large measured labels.
Instrumented Compose tests cover transparent-fill pixel colors, custom renderers and
composable labels, accessibility, empty states, bars/pies and animation-frame label
validation. Run them with the commands above.

Local verification after adding the step-chart example passed 19 unit tests and
6 instrumented tests on an API 36
emulator. The debug demo APK, release library AAR and Maven-local publication were
generated successfully, and the demo was launched on the emulator. Android Lint
reported no errors; remaining warnings concern newer dependency/SDK versions.

This version is Android-only. It intentionally does not include zoom/pan, hit testing,
tooltips, stacking/grouped bars, automatic downsampling, per-item accessibility
navigation or remote publishing. Smooth interpolation is a bounded eased segment,
not a statistical spline. Very large datasets should be aggregated before rendering;
collision checks prioritize correctness over a spatial-index implementation.
