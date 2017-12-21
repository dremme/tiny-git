package hamburg.remme.tinygit.gui.component

import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.chart.PieChart
import javafx.scene.control.Tooltip
import javafx.scene.text.Text

class PieChart(data: ObservableList<PieChart.Data>) : PieChart(data) {

    init {
        data.addListener(ListChangeListener {
            it.list.forEach {
                Tooltip.uninstall(it.node, null)
                Tooltip.install(it.node, Tooltip(it.name))
            }
        })
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        data.forEach { data ->
            val label = lookupAll(".chart-pie-label").firstOrNull { it is Text && it.text.contains(data.name) } as? Text
            label?.text = data.pieValue.toInt().toString()
        }
        super.layoutChartChildren(top, left, width, height)
    }

}
