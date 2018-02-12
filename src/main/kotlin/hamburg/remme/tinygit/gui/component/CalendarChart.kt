package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.observableList
import hamburg.remme.tinygit.shortDateFormat
import javafx.animation.FadeTransition
import javafx.collections.ObservableList
import javafx.geometry.Side
import javafx.scene.chart.XYChart
import javafx.scene.control.Tooltip
import javafx.scene.layout.StackPane
import javafx.util.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.Arrays
import java.util.Objects

class CalendarChart(data: ObservableList<Data<LocalDate, DayOfWeek>>) : XYChart<LocalDate, DayOfWeek>(DayOfYearAxis(), DayOfWeekAxis()) {

    private val padding = 2.0
    private val quarters = intArrayOf(0, 0, 0)
    private var placeholders: List<Data<LocalDate, DayOfWeek>> = emptyList()
    private var seriesHash = 0

    init {
        isLegendVisible = false
        isHorizontalGridLinesVisible = false
        isHorizontalZeroLineVisible = false
        verticalGridLinesVisible = false
        isVerticalZeroLineVisible = false
        xAxis.side = Side.TOP
        this.data = observableList(Series(data))
    }

    // TODO: implement changes periods
    fun updateYear(period: Period) {
        val lastDay = LocalDate.now()
        val firstDay = Year.of(lastDay.year - 1).atMonth(lastDay.month).atDay(1)

        plotChildren.removeAll(placeholders.map { it.node })
        placeholders = (0..ChronoUnit.DAYS.between(firstDay, lastDay))
                .map { firstDay.plusDays(it) }
                .map { Data(it, it.dayOfWeek) }
                .onEach { it.node = StackPane().addClass("chart-calendar-day", "day-0") }
        plotChildren.addAll(0, placeholders.map { it.node })
    }

    override fun seriesRemoved(series: Series<LocalDate, DayOfWeek>) = throw UnsupportedOperationException()

    override fun seriesAdded(series: Series<LocalDate, DayOfWeek>, seriesIndex: Int) {
        series.data.forEachIndexed { i, it -> dataItemAdded(series, i, it) }
    }

    override fun dataItemAdded(series: Series<LocalDate, DayOfWeek>, itemIndex: Int, item: Data<LocalDate, DayOfWeek>) {
        val value = item.extraValue as Int

        if (item.node == null) {
            item.node = StackPane()
            Tooltip.install(item.node, Tooltip("${item.xValue.format(shortDateFormat)} ($value commits)"))
        }

        val newHash = hash(series)
        if (seriesHash != newHash) {
            seriesHash = newHash
            updateQuarters(series)
        }
        val styleclass = if (value >= quarters[0]) 4 else if (value >= quarters[1]) 3 else if (value >= quarters[2]) 2 else 1
        item.node.styleClass.setAll("chart-calendar-day", "day-$styleclass")

        if (shouldAnimate()) {
            item.node.opacity = 0.0
            plotChildren += item.node
            val transition = FadeTransition(Duration.millis(250.0), item.node)
            transition.delay = Duration(xAxis.getDisplayPosition(item.xValue) / 4)
            transition.toValue = 1.0
            transition.play()
        } else {
            plotChildren += item.node
        }
    }

    private fun hash(series: Series<LocalDate, DayOfWeek>): Int {
        return Arrays.hashCode(series.data.map { Objects.hash(it.xValue, it.extraValue) }.toIntArray())
    }

    private fun updateQuarters(series: Series<LocalDate, DayOfWeek>) {
        series.data.map { it.extraValue as Int }.sortedDescending().distinct().takeIf { it.isNotEmpty() }.let {
            quarters[0] = it?.let { it[(it.size * 0.25).toInt()] } ?: 0
            quarters[1] = it?.let { it[(it.size * 0.5).toInt()] } ?: 0
            quarters[2] = it?.let { it[(it.size * 0.75).toInt()] } ?: 0
        }
    }

    override fun dataItemChanged(item: Data<LocalDate, DayOfWeek>) = throw UnsupportedOperationException()

    override fun dataItemRemoved(item: Data<LocalDate, DayOfWeek>, series: Series<LocalDate, DayOfWeek>) {
        if (shouldAnimate()) {
            val transition = FadeTransition(Duration.millis(250.0), item.node)
            transition.toValue = 0.0
            transition.setOnFinished {
                plotChildren -= item.node
                removeDataItemFromDisplay(series, item)
            }
            transition.play()
        } else {
            plotChildren -= item.node
            removeDataItemFromDisplay(series, item)
        }
    }

    override fun layoutPlotChildren() {
        val w = (xAxis as DayOfYearAxis).step
        val h = (yAxis as DayOfWeekAxis).step
        data[0].data.forEach { resizeRelocate(it, w, h) }
        placeholders.forEach { resizeRelocate(it, w, h) }
    }

    private fun resizeRelocate(data: Data<LocalDate, DayOfWeek>, w: Double, h: Double) {
        if (xAxis.isValueOnAxis(data.xValue)) {
            val x = xAxis.getDisplayPosition(data.xValue)
            val y = yAxis.getDisplayPosition(data.yValue)
            data.node.resizeRelocate(x, y, w - padding, h - padding)
        } else {
            data.node.resizeRelocate(0.0, 0.0, 0.0, 0.0)
        }
    }

    enum class Period(private val description: String) {

        LAST_YEAR("Last Year"), LAST_2_YEARS("Last Two Years");

        override fun toString() = description

    }

}
