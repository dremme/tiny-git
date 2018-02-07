package hamburg.remme.tinygit.gui.component

import javafx.collections.ObservableList
import javafx.scene.control.Tooltip
import javafx.scene.chart.PieChart as FXPieChart

class PieChart(data: ObservableList<FXPieChart.Data>, private val labelSuffix: String) : FXPieChart(data) {

    init {
        labelsVisible = false
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        data.forEach { data ->
            Tooltip.uninstall(data.node, null)
            Tooltip.install(data.node, Tooltip("${data.name} (${data.pieValue.toInt()} $labelSuffix)"))
        }
        super.layoutChartChildren(top, left, width, height)
    }

}
