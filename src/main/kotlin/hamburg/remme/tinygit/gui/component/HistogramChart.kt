package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.daysFromOrigin
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.scene.layout.Pane
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import java.time.LocalDate
import kotlin.math.max

private const val DEFAULT_STYLE_CLASS = "histogram"
private const val AXIS_STYLE_CLASS = "axis"
private const val TICK_STYLE_CLASS = "tick"
private const val SHAPE_STYLE_CLASS = "shape"
private const val RECT_STYLE_CLASS = "rectangle-color"
private const val COLOR_COUNT = 16
private const val MIN_HEIGHT = 2.0 // TODO: could cause issues
private const val TICK_MARK_LENGTH = 5.0
private const val TICK_MARK_GAP = 2.0

/** Empty days before/after the data range so the last day is not clipped at the edge. */
private const val PAD_DAYS = 7L

/**
 * @todo: find abstraction between this and [CalendarChart], especially tick marks and axes
 */
class HistogramChart(
    title: String,
) : Chart(title) {
    private val tickMarks = mutableListOf<TickMark<LocalDate>>()
    private val series = mutableListOf<Series>()
    private val data get() = series.flatMap { it.data }
    private val rectangles get() = data.map { it.node }
    private val plotContent =
        object : Pane() {
            override fun layoutChildren() {
            }
        }
    private val plotContentClip = Rectangle()
    private val xAxis = Path().addClass(AXIS_STYLE_CLASS)

    var lowerBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            lowerBoundX = value.daysFromOrigin
        }
    var upperBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            upperBoundX = value.daysFromOrigin
        }
    private var lowerBoundX = 0L
    private var upperBoundX = 100L
    private var upperBoundY = 0.0

    init {
        addClass(DEFAULT_STYLE_CLASS)
        plotContentClip.isManaged = false
        plotContentClip.isSmooth = false
        plotContent.clip = plotContentClip
        plotContent.isManaged = false
        chartChildren.addAll(plotContent, xAxis)
    }

    fun setSeries(series: List<Series>) {
        plotContent.children -= rectangles.toSet()
        this.series.clear()
        this.series += series.takeLast(COLOR_COUNT)
        this.series.forEachIndexed { i, it -> it.data.forEach { it.createNode(i) } }
        upperBoundY = data
            .groupingBy { it.xValue }
            .fold(0L) { acc, it -> acc + it.yValue }
            .values
            .maxOrNull()
            ?.toDouble()
            ?: 0.0
        plotContent.children += rectangles
        requestChartLayout()
    }

    fun setTickMarks(tickMarks: List<TickMark<LocalDate>>) {
        chartChildren -= this.tickMarks.map { it.label }.toSet()
        this.tickMarks.clear()
        this.tickMarks += tickMarks
        chartChildren += this.tickMarks.map { it.label }
    }

    /** Inclusive day span plus padding so the first/last days are fully visible. */
    private fun layoutDays() = (upperBoundX - lowerBoundX + 1 + 2 * PAD_DAYS).coerceAtLeast(1)

    private fun xOf(
        day: Long,
        stepX: Double,
    ) = (day - lowerBoundX + PAD_DAYS) * stepX

    override fun layoutChartChildren(
        width: Double,
        height: Double,
    ) {
        val stepX = width / layoutDays()
        val labelHeight = tickMarks.maxOfOrNull { it.label.prefHeight(width) } ?: 0.0
        val xAxisHeight = snapSizeY(TICK_MARK_LENGTH + TICK_MARK_GAP + labelHeight)
        val y = snapPositionY(height - xAxisHeight)
        xAxis.elements.setAll(MoveTo(0.0, 0.0), LineTo(width, 0.0))
        xAxis.relocate(0.0, y)

        tickMarks.forEach {
            var x = snapPositionX(xOf(it.value.daysFromOrigin, stepX))
            val w = snapSizeX(it.label.prefWidth(height))
            val h = snapSizeY(it.label.prefHeight(width))
            xAxis.elements.addAll(MoveTo(x, 0.0), LineTo(x, TICK_MARK_LENGTH))
            x -= if (x + w > width) w - 4.0 else 4.0
            it.label.resizeRelocate(x, y + TICK_MARK_LENGTH + TICK_MARK_GAP, w, h)
        }

        plotContentClip.width = width
        plotContentClip.height = height - xAxisHeight
        plotContent.resizeRelocate(0.0, 0.0, width, height - xAxisHeight)
        layoutPlotChildren(width, height - xAxisHeight)
    }

    private fun layoutPlotChildren(
        width: Double,
        height: Double,
    ) {
        val slots = mutableMapOf<LocalDate, Double>()
        val stepX = width / layoutDays()
        val timeline = Timeline()
        data.forEach {
            val rect = it.node as Rectangle
            if (!it.wasAnimated) {
                rect.y = height
                rect.height = 0.0
                val h = snapSizeY(max(MIN_HEIGHT, height * (it.yValue / upperBoundY.coerceAtLeast(1.0))))
                val y = snapPositionY(height - h - (slots[it.xValue] ?: 0.0))
                timeline.keyFrames +=
                    KeyFrame(
                        Duration.millis(1000.0),
                        KeyValue(rect.yProperty(), y, Interpolator.EASE_OUT),
                        KeyValue(rect.heightProperty(), h, Interpolator.EASE_OUT),
                    )
                slots[it.xValue] = (slots[it.xValue] ?: 0.0) + h
                it.wasAnimated = true
            }
            rect.x = snapPositionX(xOf(it.xValue.daysFromOrigin, stepX))
            rect.width = snapSizeX(stepX * it.size)
        }
        if (timeline.keyFrames.isNotEmpty()) timeline.play()
    }

    class TickMark<out T>(
        val name: String,
        val value: T,
    ) {
        val label =
            label {
                addClass(TICK_STYLE_CLASS)
                text = name
            }
    }

    class Series(
        val name: String,
        val data: List<Data>,
    )

    class Data(
        val xValue: LocalDate,
        val yValue: Long,
        val size: Int = 1,
    ) {
        var node: Rectangle? = null
        var wasAnimated = false

        fun createNode(index: Int) {
            node = Rectangle().apply { addClass(SHAPE_STYLE_CLASS, "$RECT_STYLE_CLASS${index % COLOR_COUNT}") }
        }
    }
}
