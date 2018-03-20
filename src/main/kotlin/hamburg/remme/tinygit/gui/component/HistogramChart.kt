package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import javafx.scene.shape.Rectangle

class HistogramChart(title: String) : Chart(title) {

    var lowerBoundX = 0L
    var upperBoundX = 100L
    private var upperBoundY = 0.0
    private val series = mutableListOf<Series>()
    private val data get() = series.flatMap { it.data }
    private val rectangles get() = data.map { it.rect }

    fun setSeries(series: Collection<Series>) {
        // Remove old rectangles first
        chartChildren.removeAll(rectangles)
        // Set new reference list
        this.series.clear()
        this.series.addAll(series)
        this.series.forEachIndexed { i, it -> it.data.forEach { it.createRect(i) } }
        // Set highest y-value
        upperBoundY = data.map { it.yValue }.max()?.toDouble() ?: 0.0
        // Add new rectangles
        chartChildren.setAll(rectangles)
        // Finally request chart layout
        requestChartLayout()
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        val stepX = width / (upperBoundX - lowerBoundX)
        data.forEach {
            val x = left + (it.xValue - lowerBoundX) * stepX
            val w = stepX
            val h = height * (it.yValue / upperBoundY)
            val y = height - h
            it.rect.x = Math.ceil(x)
            it.rect.y = y
            it.rect.width = Math.ceil(w)
            it.rect.height = h
        }
    }

    class Series(val name: String, val data: List<Data>)

    class Data(val xValue: Long, val yValue: Long) {

        lateinit var rect: Rectangle

        fun createRect(index: Int) {
            rect = Rectangle()
            rect.addClass("histogram-shape", "default-color${index % 8}")
        }

    }

}
