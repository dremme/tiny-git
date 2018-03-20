package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.HALF_PI
import hamburg.remme.tinygit.TWO_PI
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType

class DonutChart(title: String) : Chart(title) {

    private var total = 0.0
    private val data = mutableListOf<Data>()
    private val arcs get() = data.map { it.arc }

    fun setData(data: Collection<Data>) {
        // Remove old arcs first
        chartChildren.removeAll(arcs)
        // Set new reference list
        this.data.clear()
        this.data.addAll(data)
        this.data.forEachIndexed { i, it -> it.createArc(i) }
        // Set sum of values
        total = this.data.map { it.value }.sum().toDouble()
        // Add new arcs
        chartChildren.addAll(arcs)
        // Finally request chart layout
        requestChartLayout()
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        val strokeWidth = 25.0
        val radius = (Math.min(width, height) - strokeWidth) / 2.0
        var angle = 0.0
        data.forEach {
            val length = TWO_PI * (it.value / total)
            it.arc.strokeWidth = strokeWidth
            it.arc.centerX = left + width / 2
            it.arc.centerY = top + height / 2
            it.arc.radiusX = radius
            it.arc.radiusY = radius
            it.arc.startAngle = -angle + HALF_PI
            it.arc.length = -length
            angle += length
        }
    }

    class Data(val name: String, val value: Long) {

        lateinit var arc: Arc

        fun createArc(index: Int) {
            arc = Arc()
            arc.addClass("donut-shape", "default-color${index % 8}")
            arc.type = ArcType.OPEN
        }

    }

}
