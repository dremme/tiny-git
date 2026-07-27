package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.atStartOfWeek
import hamburg.remme.tinygit.dayOfWeekFormat
import hamburg.remme.tinygit.daysFromOrigin
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.scene.layout.Pane
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.Rectangle
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
private const val PAD_WEEKS = 1
private const val CELL_GAP = 2.0

/**
 * Contribution calendar: one column per week, rows Mon–Sun.
 *
 * @todo: find abstraction between this and [HistogramChart], especially tick marks and axes
 */
class CalendarChart(
    title: String,
) : Chart(title) {
    private val tickMarks = mutableListOf<TickMark<LocalDate>>()
    private val dowMarks = mutableListOf<TickMark<DayOfWeek>>()
    private val data = mutableListOf<Data>()
    private val rectangles get() = data.map { it.node }
    private val plotContent =
        object : Pane() {
            override fun layoutChildren() {
            }
        }
    private val plotContentClip = Rectangle()
    private val xAxis = Path().addClass(AXIS_STYLE_CLASS)

    private var firstWeek = 0L
    private var weekCount = 1

    var lowerBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            firstWeek = value.atStartOfWeek().daysFromOrigin
            refreshWeekCount()
        }
    var upperBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            lastDay = value.daysFromOrigin
            refreshWeekCount()
        }
    private var lastDay = 100L

    init {
        addClass(DEFAULT_STYLE_CLASS)
        plotContentClip.isManaged = false
        plotContentClip.isSmooth = false
        plotContent.clip = plotContentClip
        plotContent.isManaged = false
        chartChildren.addAll(plotContent, xAxis)
        setDowMarks(DayOfWeek.entries.map { TickMark(dayOfWeekFormat.format(it), it) })
    }

    private fun refreshWeekCount() {
        val lastWeek = lastDay - ((lastDay - firstWeek) % 7)
        weekCount = (((lastWeek - firstWeek) / 7) + 1).toInt().coerceAtLeast(1)
    }

    fun setData(data: List<Data>) {
        plotContent.children -= rectangles.toSet()
        val maxY = data.maxOfOrNull { it.yValue }?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
        this.data.clear()
        this.data += data
        this.data.forEach { it.createNode((it.yValue / maxY * 4).roundToInt().coerceIn(0, 4)) }
        plotContent.children += rectangles
        requestChartLayout()
    }

    fun setTickMarks(tickMarks: List<TickMark<LocalDate>>) {
        chartChildren -= this.tickMarks.map { it.label }.toSet()
        this.tickMarks.clear()
        this.tickMarks += tickMarks
        chartChildren += this.tickMarks.map { it.label }
        requestChartLayout()
    }

    private fun setDowMarks(tickMarks: List<TickMark<DayOfWeek>>) {
        chartChildren -= this.dowMarks.map { it.label }.toSet()
        this.dowMarks.clear()
        this.dowMarks += tickMarks
        chartChildren += this.dowMarks.map { it.label }
    }

    private fun layoutWeeks() = weekCount + 2 * PAD_WEEKS

    private fun weekCol(day: Long): Int = (((day.atWeekStart() - firstWeek) / 7).toInt() + PAD_WEEKS).coerceIn(0, layoutWeeks() - 1)

    private fun Long.atWeekStart() = this - ((this - firstWeek) % 7 + 7) % 7

    override fun layoutChartChildren(
        width: Double,
        height: Double,
    ) {
        val labelWidth = dowMarks.maxOfOrNull { it.label.prefWidth(height) } ?: 0.0
        val labelHeight = tickMarks.maxOfOrNull { it.label.prefHeight(width) } ?: 0.0
        val yAxisWidth = snapSizeX(TICK_MARK_LENGTH + labelWidth)
        val xAxisHeight = snapSizeY(TICK_MARK_LENGTH + TICK_MARK_GAP + labelHeight)
        val contentWidth = width - yAxisWidth
        val contentHeight = height - xAxisHeight
        val stepX = contentWidth / layoutWeeks()
        val stepY = contentHeight / 7

        xAxis.elements.clear()
        tickMarks.forEach {
            var x = snapPositionX(weekCol(it.value.daysFromOrigin) * stepX + yAxisWidth)
            val w = snapSizeX(it.label.prefWidth(contentHeight))
            val h = snapSizeY(it.label.prefHeight(contentWidth))
            xAxis.elements.addAll(MoveTo(x, height - xAxisHeight), LineTo(x, height - xAxisHeight + TICK_MARK_LENGTH))
            x -= if (x + w > width) w - 4.0 else 4.0
            it.label.resizeRelocate(x, height - xAxisHeight + TICK_MARK_LENGTH + TICK_MARK_GAP, w, h)
        }
        dowMarks.forEach {
            val y = snapPositionY(it.value.ordinal * stepY + stepY / 2)
            val w = snapSizeX(it.label.prefWidth(contentHeight))
            val h = snapSizeY(it.label.prefHeight(contentWidth))
            it.label.resizeRelocate(yAxisWidth - w - TICK_MARK_LENGTH, y - h / 2, w, h)
        }

        plotContentClip.width = contentWidth
        plotContentClip.height = contentHeight
        plotContent.resizeRelocate(yAxisWidth, 0.0, contentWidth, contentHeight)
        layoutPlotChildren(contentWidth, contentHeight)
    }

    private fun layoutPlotChildren(
        width: Double,
        height: Double,
    ) {
        val stepX = width / layoutWeeks()
        val stepY = height / 7
        data.forEach {
            val rect = it.node as Rectangle
            val x = weekCol(it.xValue.daysFromOrigin) * stepX
            val y = it.xValue.dayOfWeek.ordinal * stepY
            rect.opacity = 1.0
            rect.x = snapPositionX(x) + CELL_GAP / 2
            rect.y = snapPositionY(y) + CELL_GAP / 2
            rect.width = (snapSizeX(stepX) - CELL_GAP).coerceAtLeast(1.0)
            rect.height = (snapSizeY(stepY) - CELL_GAP).coerceAtLeast(1.0)
        }
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

    class Data(
        val xValue: LocalDate,
        val yValue: Int,
    ) {
        var node: Rectangle? = null
        var index = 0

        fun createNode(index: Int) {
            this.index = index
            node = Rectangle().apply { addClass(SHAPE_STYLE_CLASS, "$RECT_STYLE_CLASS$index") }
        }
    }
}
