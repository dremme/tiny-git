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

private const val DEFAULT_STYLE_CLASS = "histogram"
private const val AXIS_STYLE_CLASS = "axis"
private const val TICK_STYLE_CLASS = "tick"
private const val SHAPE_STYLE_CLASS = "shape"
private const val RECT_STYLE_CLASS = "rectangle-color"
private const val COLOR_COUNT = 16
private const val MIN_HEIGHT = 2.0 // TODO: could cause issues
private const val TICK_MARK_LENGTH = 5.0
private const val TICK_MARK_GAP = 2.0

class HistogramChart(title: String) : Chart(title) {

    private val tickMarks = mutableListOf<TickMark<LocalDate>>()
    private val series = mutableListOf<Series>()
    private val data get() = series.flatMap { it.data }
    private val rectangles get() = data.map { it.node }
    private val plotContent = object : Pane() {
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
        // Remove old rectangles first
        plotContent.children -= rectangles
        // Set new reference list
        this.series.clear()
        this.series += series.takeLast(COLOR_COUNT)
        this.series.forEachIndexed { i, it -> it.data.forEach { it.createNode(i) } }
        // Set highest y-value
        upperBoundY = data.groupingBy { it.xValue }
                .fold(0L) { acc, it -> acc + it.yValue }
                .map { it.value }
                .max()?.toDouble()
                ?: 0.0
        // Add new rectangles
        plotContent.children += rectangles
        // Finally request chart layout
        requestChartLayout()
    }

    fun setTickMarks(tickMarks: List<TickMark<LocalDate>>) {
        // Remove old labels
        chartChildren -= this.tickMarks.map { it.label }
        // Set new reference list
        this.tickMarks.clear()
        this.tickMarks += tickMarks
        // Add new labels
        chartChildren += this.tickMarks.map { it.label }
    }

    override fun layoutChartChildren(width: Double, height: Double) {
        val stepX = width / (upperBoundX - lowerBoundX)

        val labelHeight = tickMarks.map { it.label.prefHeight(width) }.max() ?: 0.0
        val xAxisHeight = snapSizeY(TICK_MARK_LENGTH + TICK_MARK_GAP + labelHeight)
        val y = snapPositionY(height - xAxisHeight)
        xAxis.elements.setAll(MoveTo(0.0, 0.0), LineTo(width, 0.0))
        xAxis.relocate(0.0, y)

        tickMarks.forEach {
            val x = snapPositionX((it.value.daysFromOrigin - lowerBoundX) * stepX)
            val w = snapSizeX(it.label.prefWidth(height))
            val h = snapSizeY(it.label.prefHeight(width))
            xAxis.elements.addAll(MoveTo(x, 0.0), LineTo(x, TICK_MARK_LENGTH))
            it.label.resizeRelocate(x, y + TICK_MARK_LENGTH + TICK_MARK_GAP, w, h)
        }

        plotContentClip.x = 0.0
        plotContentClip.y = 0.0
        plotContentClip.width = width
        plotContentClip.height = height - xAxisHeight
        plotContent.resizeRelocate(0.0, 0.0, width, height - xAxisHeight)

        layoutPlotChildren(width, height - xAxisHeight)
    }

    private fun layoutPlotChildren(width: Double, height: Double) {
        val slots = mutableMapOf<LocalDate, Double>()
        val stepX = width / (upperBoundX - lowerBoundX)
        val timeline = Timeline()
        data.forEach {
            val rect = it.node as Rectangle
            if (!it.wasAnimated) {
                rect.y = height
                rect.height = 0.0

                val h = snapSizeY(Math.max(MIN_HEIGHT, height * (it.yValue / upperBoundY)))
                val y = snapPositionY(height - h - (slots[it.xValue] ?: 0.0))
                timeline.keyFrames += KeyFrame(Duration.millis(1000.0),
                        KeyValue(rect.yProperty(), y, Interpolator.EASE_OUT),
                        KeyValue(rect.heightProperty(), h, Interpolator.EASE_OUT))

                slots[it.xValue] = slots[it.xValue] ?: 0.0 + h
                it.wasAnimated = true
            }
            val x = (it.xValue.daysFromOrigin - lowerBoundX) * stepX
            val w = stepX * it.size
            rect.x = snapPositionX(x)
            rect.width = snapSizeX(w)
        }
        if (timeline.keyFrames.isNotEmpty()) timeline.play()
    }

    class TickMark<out T>(val name: String, val value: T) {

        val label = label {
            addClass(TICK_STYLE_CLASS)
            +name
        }

    }

    class Series(val name: String, val data: List<Data>)

    class Data(val xValue: LocalDate, val yValue: Long, val size: Int = 1) {

        var node: Rectangle? = null
        var wasAnimated = false

        fun createNode(index: Int) {
            node = Rectangle().apply { addClass(SHAPE_STYLE_CLASS, "$RECT_STYLE_CLASS${index % COLOR_COUNT}") }
        }

    }

}
