# `:charts` — Compose chart library

Reusable Android charts built on Compose Canvas and layout primitives, with no
Material dependency. This module owns models, styling, geometry, animation,
collision-aware labels, chart rendering and its tests. The [`:app` gallery](../app/src/main/java/com/charting/charting/MainActivity.kt)
is an example consumer, not part of the library API.

## Requirements and installation

| Setting | Value |
| --- | --- |
| Namespace | `com.charting.charts` |
| Maven coordinates | `com.charting:charts-compose:0.1.0` |
| Minimum Android API | 24 |
| Library compile SDK | 36 |
| Build JDK | 21 |
| Java source/target compatibility | 11 |
| Compose BOM | `2024.11.00` |
| Compose compiler plugin | `2.3.21` |

The repository uses AGP 9 built-in Kotlin. Keep plugin and dependency versions in
the [version catalog](../gradle/libs.versions.toml). Consumers need Compose enabled
and compatible Compose/Kotlin tooling. UI, Foundation and Animation Core are public
dependencies; the module does not force a Material theme.

For another module in this repository:

```kotlin
dependencies {
    implementation(project(":charts"))
}
```

To consume the release AAR from another local project, run
`./gradlew :charts:publishReleasePublicationToMavenLocal` from the repository root,
add `mavenLocal()` to that consumer's dependency repositories, and depend on
`com.charting:charts-compose:0.1.0`. The publication includes sources. No remote
Maven publication or release signing is configured. Version `0.1.0` is experimental;
source and binary compatibility are not yet guaranteed.

## Public API map

All packages below are prefixed with `com.charting.charts`.

| Package | Main types | Responsibility |
| --- | --- | --- |
| [`model`](src/main/java/com/charting/charts/model/ChartData.kt) | `ChartPoint`, `ChartSeries`, `BarEntry`, `PieSlice`, `ChartRange` | Validated chart input |
| [`style`](src/main/java/com/charting/charts/style/ChartStyle.kt) | `ChartTheme`, `ChartThemeData`, `LineChartStyle`, `LineFillStyle`, `FillSection`, `FillPaint`, `FillBaseline`, `PointStyle`, `BarStyle`, `PieStyle`, `LabelStyle`, `AxisStyle` | Appearance and defaults |
| [`ui`](src/main/java/com/charting/charts/ui) | `LineChart`, `BarChart`, `PieChart` | Ready-to-use composables |
| [`core`](src/main/java/com/charting/charts/core) | `ChartSurface`, `ChartRenderer`, `ChartRenderContext`, `ChartScene`, `ChartLayer`, `ChartObstacle`, `CartesianScale` | Shared rendering and geometry |
| [`labels`](src/main/java/com/charting/charts/labels/LabelPlacement.kt) | `LabelPlacementStrategy`, `StaggeredLabelPlacement`, `LabelPlacementRequest`, `LabelCandidate` | Measured label placement |
| [`animation`](src/main/java/com/charting/charts/animation/ChartAnimation.kt) | `ChartAnimationStrategy`, `GrowingChartAnimation`, `NoChartAnimation`, `AnimatedChartValue`, `ChartValueId` | Entry and update animation |

## Example: blue steps with one filled plateau

The following belongs inside a composable. Import models from `.model`, styles
from `.style`, `LineChart` from `.ui`, and the standard Compose types from AndroidX.

```kotlin
val points = listOf(
    ChartPoint("start", 0.0, 100.0),
    ChartPoint("drop-top", 8.0, 100.0),
    ChartPoint("drop-bottom", 8.0, 50.0, label = "50"),
    ChartPoint("rise-bottom", 16.0, 50.0),
    ChartPoint("rise-top", 16.0, 75.0),
    ChartPoint("end", 24.0, 75.0),
)

LineChart(
    series = listOf(ChartSeries("steps", points)),
    modifier = Modifier.fillMaxWidth().height(360.dp),
    style = LineChartStyle(
        color = Color.Blue,
        dots = PointStyle(color = Color.Blue),
        interpolation = LineInterpolation.Linear,
        fill = LineFillStyle(
            sections = listOf(
                FillSection("drop-bottom", "rise-bottom", FillPaint(Color.Blue, 0.25f)),
            ),
            baseline = FillBaseline.Zero,
        ),
    ),
)
```

This draws vertical edges at X=8 and X=16. Only X=8–16 is filled down to zero.
Only `(8,50)` requests a label; the label may move upward to avoid the vertical
edge and other geometry. A section between `(8,100)` and `(8,50)` would have zero
area, not a visible rectangular band. Identify endpoints by ID, not by X alone.

For other chart types, supply `List<BarEntry>` to `BarChart` and `List<PieSlice>` to
`PieChart`. Bars support negative values and per-entry `BarStyle`; pies accept
non-negative values and per-slice `FillPaint`. `PieStyle(holeFraction = 0.35f)`
produces a donut. All chart composables accept a modifier, label style and placement
policy, animation strategy, custom label content and accessibility description.

## Input and styling contracts

- IDs must be non-blank and unique in their scope. Point IDs are unique within a
  series; series, bar and slice IDs are unique within their chart. Retain IDs across
  updates and submit new lists rather than mutating existing input in place.
- Coordinates and values must be finite. Line X values are non-decreasing;
  repeated X values retain list order and form vertical edges. Decreasing X fails
  validation. Empty input and single-point series are supported.
- `ChartRange` requires finite bounds, `min < max`, and a representable span.
  Automatic ranges expand constant values; automatic Y ranges include zero and
  currently animated values. Explicit ranges override automatic domain selection.
- Pie values cannot be negative. Zero-valued wedges are not drawn; an all-zero pie
  is empty. Large finite values are normalized before summation to avoid overflow.
- `LineChartStyle` controls line color, width, dashes, marker defaults,
  interpolation and fill. A point can override its marker using `PointStyle`.
  Smooth interpolation is a bounded eased segment, not a statistical spline.
- `FillPaint` accepts a `Color` or Compose `Brush` and an alpha in `[0, 1]`.
  Brush coordinates are in the chart's pixel space, not local to a fill section.
- A fill section's start ID must precede its end ID in the series. Unknown IDs,
  reversed endpoints or identical endpoint IDs are rejected. Later sections own
  overlapping edges: paints are replaced, not stacked. `full` fills any remaining
  edges; `full = null` leaves them empty. Different series still composite normally.
- `FillBaseline.Bottom`, `Zero` and `Value(y)` select the closing baseline. It is
  clamped to the plot. Negative values fill the area between curve and baseline,
  which can lie visually above the curve.
- Defaults come from `ChartTheme` / `LocalChartTheme`. Explicit chart, series and
  element overrides take precedence where supported. Automatic line series use
  the theme palette for line/marker colors when no chart or series style is supplied.
  Override objects replace the corresponding style; unspecified fields use that
  object's constructor defaults, not a field-by-field merge with the theme.
- Use `copy` to derive style presets. Supply text, axis and chart colors appropriate
  for the application's light/dark theme; no Material colors are inferred.

## Labels and accessibility

Line point labels are opt-in (`label = null` hides them). Bar labels combine their
category and formatted value; pie labels use the slice label. `labelContent` can
replace the default text with a composable. Its drawing must stay within its measured
bounds; out-of-bounds shadows and offsets cannot be collision-checked.

The default strategy measures real content, orders candidates by descending priority
then stable ID, keeps X fixed, and tries progressively higher positions. It avoids
other labels and data geometry from every series, including line thickness, markers,
fills, bars, pie wedges and axis tick text. Grid lines are background decoration,
not obstacles. Dashed trajectories are checked conservatively as continuous lines.

`LabelStyle` exposes `gap`, `levelSpacing`, `maxLevels`, `reservedTopSpace`, text
style and visibility. The reserved top space is inside the chart's supplied height.
When no candidate fits, a label is hidden rather than allowed to overlap. Labels
near left/right boundaries can hide because the default does not shift them sideways.

Placement is rechecked against each animated geometry frame. Labels snap or hide;
they do not interpolate across obstacles. The chart semantics retain all data values
even if labels are hidden. Pass a localized `contentDescription` when needed.
Accessibility currently uses one chart summary, not separately focusable data points.

## Animation lifecycle

`DefaultChartAnimation` uses `GrowingChartAnimation` with a 600 ms tween. New Y
values grow from zero; existing X/Y values interpolate by `ChartValueId`. An
interrupted update continues from the last displayed frame. Removed IDs disappear
immediately; style changes are immediate. Pie entrance also animates its sweep.

Pass `NoChartAnimation` for static rendering or use another finite Compose spec
with `GrowingChartAnimation`. Its fractions are clamped to prevent invalid negative
pie values from overshoot, and Compose's animation duration scale is respected.
Keep custom strategy instances stable across recompositions.

## Architecture and extension contracts

The rendering flow is **validated data → animated values → scaled geometry → scene
layers and obstacles → measured labels → placement → Canvas and composable content**.
Line paths and fill collision polygons share sampled geometry, avoiding a mismatch
between the visible curve and label obstacles.

| Extension | Contract |
| --- | --- |
| New chart | Implement `ChartRenderer.createScene(context)` and pass it to `ChartSurface`. Return layers, obstacles and label anchors. Context includes size in pixels, density, text measurement and label style. |
| New drawing/style | Capture a custom style object in the renderer and return ordered `ChartLayer` instances. Each layer draws in a Compose `DrawScope`. No closed chart hierarchy is required. |
| New obstacle | Implement `ChartObstacle.intersects(bounds)`. Match the geometry actually drawn; the host cannot derive obstacles from drawing commands. Rectangle, polygon and polyline implementations are supplied. |
| New label policy | Implement `LabelPlacementStrategy.place(request)`, returning pixel bounds keyed by candidate ID. Omit an ID to hide it. Enforce chart bounds and obstacle separation yourself. |
| New animation | Implement the composable `ChartAnimationStrategy.values(target)`. Return every target ID with finite coordinates; pie values must remain non-negative. |

Scene label IDs must be unique. Use physical chart coordinates: increasing X remains
left-to-right even in RTL applications. Custom rendering/placement is responsible
for its own collision guarantees. Avoid expensive I/O or mutable external state
in scene construction, drawing and placement callbacks.

## Tests and development

Run from the repository root:

```shell
./gradlew :charts:testDebugUnitTest
./gradlew :charts:connectedDebugAndroidTest
./gradlew :charts:lintDebug :charts:assembleRelease
./gradlew :app:assembleDebug
```

Instrumented tests need a connected emulator/device (the current suite has been run
on API 36). See [contributing](../CONTRIBUTING.md) for the full verification command.

| Test file | Coverage |
| --- | --- |
| [`GeometryTest`](src/test/java/com/charting/charts/GeometryTest.kt) | Model validation, scales, constant/empty ranges, fill ownership, polygon/line intersections, smoothing, repeated-X steps and pie normalization |
| [`LabelPlacementTest`](src/test/java/com/charting/charts/LabelPlacementTest.kt) | Escalating heights, cross-series obstacles, priority, hidden labels, deterministic ordering and dense/large labels |
| [`ChartComposeTest`](src/androidTest/java/com/charting/charts/ChartComposeTest.kt) | Actual fill/step pixel colors, one safe step label, empty states, accessibility, custom renderers/labels, animation frames, bars and pies |

Publication tasks and dependencies are defined in [`build.gradle.kts`](build.gradle.kts).
Output artifacts are under `build/outputs/aar/`; test and lint reports are under
`build/reports/`. Generated output must not be committed.

## Performance and current limits

Geometry and label placement are evaluated during updates. With L labels, K candidate
levels and G obstacle segments, placement can approach O(L × K × (G + L)); there is
no spatial index or automatic downsampling. Aggregate very large datasets upstream
and disable unnecessary labels. Curve sampling is bounded per segment.

The current scope is Android-only. Zoom/pan, hit testing, tooltips, stacked/grouped
bars, animated removals, separate per-item accessibility navigation and remote Maven
publishing are not implemented. There is no guarantee that every requested label can
fit a bounded chart. Add regression tests when changing geometry, labels or animation,
and document any changes to these public contracts.
