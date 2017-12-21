package hamburg.remme.tinygit.gui.component

import javafx.collections.ObservableList
import javafx.scene.chart.PieChart
import javafx.scene.control.Tooltip

class PieChart(data: ObservableList<PieChart.Data>, private val labelSuffix: String) : PieChart(data) {

    init {
        labelsVisible = false
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        data.forEach { data ->
            Tooltip.uninstall(data.node, null)
            Tooltip.install(data.node, Tooltip("${data.pieValue.toInt()} $labelSuffix"))
        }
        super.layoutChartChildren(top, left, width, height)
    }

}
