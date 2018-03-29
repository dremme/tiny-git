package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.atStartOfWeek
import hamburg.remme.tinygit.dayOfWeekFormat
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
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

private const val DEFAULT_STYLE_CLASS = "calendar"
private const val AXIS_STYLE_CLASS = "axis"
private const val TICK_STYLE_CLASS = "tick"
private const val SHAPE_STYLE_CLASS = "shape"
private const val RECT_STYLE_CLASS = "rectangle-color"
private const val TICK_MARK_LENGTH = 5.0
private const val TICK_MARK_GAP = 2.0

/**
 * @todo: find abstract between this and [HistogramChart], especially tick marks and axes
 */
class CalendarChart(title: String) : Chart(title) {

    private val tickMarks = mutableListOf<TickMark<LocalDate>>()
    private val dowMarks = mutableListOf<TickMark<DayOfWeek>>()
    private val data = mutableListOf<Data>()
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

    init {
        addClass(DEFAULT_STYLE_CLASS)
        plotContentClip.isManaged = false
        plotContentClip.isSmooth = false
        plotContent.clip = plotContentClip
        plotContent.isManaged = false
        chartChildren.addAll(plotContent, xAxis)
        setDowMarks(DayOfWeek.values().map { TickMark(dayOfWeekFormat.format(it), it) })
    }

    fun setData(data: List<Data>) {
        // Remove old rectangles first
        plotContent.children -= rectangles
        // Get highest y-value
        val maxY = data.map { it.yValue }.max()?.toDouble() ?: 0.0
        // Set new reference list
        this.data.clear()
        this.data += data
        this.data.forEach { it.createNode((it.yValue / maxY * 4).roundToInt()) }
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

    private fun setDowMarks(tickMarks: List<TickMark<DayOfWeek>>) {
        // Remove old labels
        chartChildren -= this.dowMarks.map { it.label }
        // Set new reference list
        this.dowMarks.clear()
        this.dowMarks += tickMarks
        // Add new labels
        chartChildren += this.dowMarks.map { it.label }
    }

    override fun layoutChartChildren(width: Double, height: Double) {
        val labelWidth = dowMarks.map { it.label.prefWidth(height) }.max() ?: 0.0
        val labelHeight = tickMarks.map { it.label.prefHeight(width) }.max() ?: 0.0
        val yAxisWidth = snapSizeX(TICK_MARK_LENGTH + labelWidth)
        val xAxisHeight = snapSizeY(TICK_MARK_LENGTH + TICK_MARK_GAP + labelHeight)

        val contentWidth = width - yAxisWidth
        val contentHeight = height - xAxisHeight
        val stepX = contentWidth / (upperBoundX - lowerBoundX)
        val stepY = contentHeight / 7

        xAxis.elements.clear()

        tickMarks.forEach {
            val relativeDay = Math.max(0, it.value.atStartOfWeek().daysFromOrigin - lowerBoundX)
            val x = snapPositionX(relativeDay * stepX + yAxisWidth)
            val w = snapSizeX(it.label.prefWidth(contentHeight))
            val h = snapSizeY(it.label.prefHeight(contentWidth))
            xAxis.elements.addAll(MoveTo(x, height - xAxisHeight), LineTo(x, height - xAxisHeight + TICK_MARK_LENGTH))
            it.label.resizeRelocate(x, height - xAxisHeight + TICK_MARK_LENGTH + TICK_MARK_GAP, w, h)
        }
        dowMarks.forEach {
            val y = snapPositionY(it.value.value * stepY - stepY / 2)
            val w = snapSizeX(it.label.prefWidth(contentHeight))
            val h = snapSizeY(it.label.prefHeight(contentWidth))
            it.label.resizeRelocate(yAxisWidth - w - TICK_MARK_LENGTH, y - h / 2, w, h)
        }

        plotContentClip.x = 0.0
        plotContentClip.y = 0.0
        plotContentClip.width = contentWidth
        plotContentClip.height = contentHeight
        plotContent.resizeRelocate(yAxisWidth, 0.0, contentWidth, contentHeight)

        layoutPlotChildren(contentWidth, contentHeight)
    }

    private fun layoutPlotChildren(width: Double, height: Double) {
        val stepX = width / (upperBoundX - lowerBoundX)
        val stepY = height / 7
        val timeline = Timeline()
        data.forEach {
            val rect = it.node as Rectangle
            if (!it.wasAnimated) {
                rect.opacity = 0.0
                timeline.keyFrames += KeyFrame(Duration.millis(500.0 + 500.0 * it.index),
                        KeyValue(rect.opacityProperty(), 1.0, Interpolator.EASE_OUT))
                it.wasAnimated = true
            }
            val adjustedDate = it.xValue.atStartOfWeek()
            val x = (adjustedDate.daysFromOrigin - lowerBoundX) * stepX
            val y = it.xValue.dayOfWeek.ordinal * stepY
            val w = stepX * 7
            val h = stepY
            rect.x = snapPositionX(x) + 1
            rect.y = snapPositionY(y) + 1
            rect.width = snapSizeX(w) - 2
            rect.height = snapSizeY(h) - 2
        }
        if (timeline.keyFrames.isNotEmpty()) timeline.play()
    }

    class TickMark<out T>(val name: String, val value: T) {

        val label = label {
            addClass(TICK_STYLE_CLASS)
            +name
        }

    }

    class Data(val xValue: LocalDate, val yValue: Int) {

        var node: Rectangle? = null
        var wasAnimated = false
        var index = 0

        fun createNode(index: Int) {
            this.index = index
            node = Rectangle().apply { addClass(SHAPE_STYLE_CLASS, "$RECT_STYLE_CLASS$index") }
        }

    }

}
