package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.numberOfWeeks
import javafx.geometry.Side
import javafx.scene.chart.Axis
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

class DayOfYearAxis : Axis<LocalDate>() {

    var step: Double = 0.0
        private set
    private lateinit var today: LocalDate
    private lateinit var currentYear: Year
    private var extraWeek: Int = 0
    private var length: Double = 0.0

    init {
        side = Side.TOP
    }

    override fun invalidateRange(data: List<LocalDate>) {
        if (data.size == 1) setRange(Year.of(data.map { it.year }.distinct()[0]), shouldAnimate())
    }

    override fun autoRange(length: Double): Any {
        this.length = length
        today = LocalDate.now()
        currentYear = Year.now()
        return currentYear
    }

    override fun getRange() = Unit

    override fun setRange(range: Any, animate: Boolean) {
        currentYear = range as Year
        extraWeek = if (currentYear.atDay(1).dayOfWeek > DayOfWeek.THURSDAY) 1 else 0
        step = length / (currentYear.numberOfWeeks() + extraWeek)
    }

    override fun getTickMarkLabel(date: LocalDate) = date.month.getDisplayName(TextStyle.SHORT, Locale.ROOT)!!

    override fun calculateTickValues(length: Double, range: Any) = (1..12).map { currentYear.atMonth(it).atDay(1) }

    override fun getDisplayPosition(date: LocalDate): Double {
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return if (date.month == Month.JANUARY && week >= 52) 0.0 else (week + extraWeek - 1) * step
    }

    override fun getValueForDisplay(displayPosition: Double) = throw UnsupportedOperationException()

    override fun isValueOnAxis(date: LocalDate) = !date.isAfter(today) && date.year <= currentYear.value

    override fun getZeroPosition() = 0.0

    override fun toNumericValue(date: LocalDate) = throw UnsupportedOperationException()

    override fun toRealValue(value: Double) = throw UnsupportedOperationException()

}
