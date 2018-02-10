package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.atEndOfDay
import hamburg.remme.tinygit.atNoon
import hamburg.remme.tinygit.weeksBetween
import javafx.geometry.Side
import javafx.scene.chart.Axis
import java.time.LocalDate
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale

class DayOfYearAxis : Axis<LocalDate>() {

    var step: Double = 0.0
        private set
    private lateinit var firstDay: LocalDate
    private lateinit var lastDay: LocalDate
    private var length: Double = 0.0

    init {
        side = Side.TOP
    }

    override fun autoRange(length: Double): Any {
        this.length = length
        val now = LocalDate.now()
        lastDay = Year.of(now.year).atMonth(now.month).atEndOfMonth()
        firstDay = Year.of(now.year - 1).atMonth(now.month).atDay(1)
        return firstDay to lastDay
    }

    override fun getRange() = firstDay to lastDay

    override fun setRange(range: Any, animate: Boolean) {
        @Suppress("UNCHECKED_CAST") (range as Pair<LocalDate, LocalDate>).let {
            firstDay = it.first
            lastDay = it.second
        }
        step = length / firstDay.weeksBetween(lastDay)
    }

    override fun getTickMarkLabel(date: LocalDate) = "${date.month.getDisplayName(TextStyle.SHORT, Locale.ROOT)} '${date.year.toString().substring(2)}"

    override fun calculateTickValues(length: Double, range: Any) = (0..12).map { firstDay.plusMonths(it.toLong()) }

    override fun getDisplayPosition(date: LocalDate) = firstDay.weeksBetween(date) * step

    override fun getValueForDisplay(displayPosition: Double) = throw UnsupportedOperationException()

    override fun isValueOnAxis(date: LocalDate) = date.atNoon().isAfter(firstDay.atStartOfDay()) && date.atNoon().isBefore(lastDay.atEndOfDay())

    override fun getZeroPosition() = 0.0

    override fun toNumericValue(date: LocalDate) = throw UnsupportedOperationException()

    override fun toRealValue(value: Double) = throw UnsupportedOperationException()

}
