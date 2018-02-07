package hamburg.remme.tinygit.gui.component

import javafx.scene.chart.Axis

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class DayOfWeekAxis : Axis<DayOfWeek>() {

    var step: Double = 0.0
        private set

    init {
        isAutoRanging = false
    }

    override fun autoRange(length: Double) {
        step = length / 7
    }

    override fun getRange() = Unit

    override fun setRange(range: Any, animate: Boolean) = Unit

    override fun getTickMarkLabel(dayOfWeek: DayOfWeek) = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ROOT)!!

    override fun calculateTickValues(length: Double, range: Any) = DayOfWeek.values().toList().reversed()

    override fun getDisplayPosition(dayOfWeek: DayOfWeek) = (dayOfWeek.value - 1) * step

    override fun getValueForDisplay(displayPosition: Double) = throw UnsupportedOperationException()

    override fun isValueOnAxis(dayOfWeek: DayOfWeek) = true

    override fun getZeroPosition() = 0.0

    override fun toRealValue(value: Double) = throw UnsupportedOperationException()

    override fun toNumericValue(dayOfWeek: DayOfWeek) = throw UnsupportedOperationException()

}
