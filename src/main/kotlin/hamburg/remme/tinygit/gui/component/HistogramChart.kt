package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.daysFromOrigin
import hamburg.remme.tinygit.gui.builder.addClass
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import java.time.LocalDate

private const val COLOR_COUNT = 10

class HistogramChart(title: String) : Chart(title) {

    private val series = mutableListOf<Series>()
    private val data get() = series.flatMap { it.data }
    private val rectangles get() = data.map { it.node }

    var lowerBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            lowerBoundX = value.daysFromOrigin
        }
    var upperBound: LocalDate
        get() = throw RuntimeException("Write-only property.")
        set(value) {
            upperBoundX = value.daysFromOrigin
        }
    private var lowerBoundX = 0L
    private var upperBoundX = 100L
    private var upperBoundY = 0.0

    fun setSeries(series: List<Series>) {
        // Remove old rectangles first
        chartChildren.removeAll(rectangles)
        // Set new reference list
        this.series.clear()
        this.series.addAll(series.takeLast(COLOR_COUNT))
        this.series.forEachIndexed { i, it -> it.data.forEach { it.createNode(i) } }
        // Set highest y-value
        upperBoundY = data.groupingBy { it.xValue }
                .fold(0L) { acc, it -> acc + it.yValue }
                .map { it.value }
                .max()?.toDouble()
                ?: 0.0
        // Add new rectangles
        chartChildren.setAll(rectangles)
        // Finally request chart layout
        requestChartLayout()
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        val slots = mutableMapOf<LocalDate, Double>()
        val stepX = width / (upperBoundX - lowerBoundX)
        val timeline = Timeline()
        data.forEach {
            val rect = it.node as Rectangle
            if (!it.wasAnimated) {
                rect.y = height
                rect.height = 0.0

                val h = height * (it.yValue / upperBoundY)
                val y = height - h - (slots[it.xValue] ?: 0.0)
                val yKeyValue = KeyValue(rect.yProperty(), y, Interpolator.EASE_OUT)
                val heightKeyValue = KeyValue(rect.heightProperty(), h, Interpolator.EASE_OUT)
                timeline.keyFrames.add(KeyFrame(Duration.millis(1000.0), yKeyValue, heightKeyValue))

                slots[it.xValue] = slots[it.xValue] ?: 0.0 + h
                it.wasAnimated = true
            }
            val x = left + (it.xValue.daysFromOrigin - lowerBoundX) * stepX
            val w = stepX * it.size
            rect.x = Math.floor(x)
            rect.width = Math.ceil(w)
        }
        if (timeline.keyFrames.isNotEmpty()) timeline.play()
    }

    class Series(val name: String, val data: List<Data>)

    class Data(val xValue: LocalDate, val yValue: Long, val size: Int = 1) {

        var node: Rectangle? = null
        var wasAnimated = false

        fun createNode(index: Int) {
            node = Rectangle().apply { addClass("histogram-shape", "default-color${index % COLOR_COUNT}") }
        }

    }

}
