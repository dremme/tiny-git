package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.HALF_PI
import hamburg.remme.tinygit.TWO_PI
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.util.Duration

private const val COLOR_COUNT = 10

class DonutChart(title: String) : Chart(title) {

    private val data = mutableListOf<Data>()
    private val arcs get() = data.map { it.node }
    private val valueLabel = label { addClass("diagram-value") }
    private val descriptionLabel = label { addClass("diagram-description") }
    private var total = 0.0

    init {
        chartChildren.addAll(valueLabel, descriptionLabel)
    }

    fun setData(data: List<Data>, description: (Long) -> String) {
        // Remove old arcs first
        chartChildren.removeAll(arcs)
        // Set new reference list
        this.data.clear()
        this.data.addAll(data.takeLast(COLOR_COUNT))
        this.data.forEachIndexed { i, it -> it.createNode(i) }
        // Set sum of values
        total = this.data.map { it.value }.sum().toDouble()
        // Set labels with original data
        val sum = data.map { it.value }.sum() // not this.data!
        valueLabel.text = sum.toString()
        descriptionLabel.text = "...${description(sum)}"
        // Add new arcs
        chartChildren.addAll(arcs)
        // Finally request chart layout
        requestChartLayout()
    }

    override fun layoutChartChildren(top: Double, left: Double, width: Double, height: Double) {
        val valueHeight = snapSizeY(valueLabel.prefHeight(width))
        valueLabel.resizeRelocate(left, top + height / 2 - valueHeight / 2, width, valueHeight)

        val descriptionHeight = snapSizeY(descriptionLabel.prefHeight(width))
        descriptionLabel.resizeRelocate(left, top + height / 2 + valueHeight / 2, width, descriptionHeight)

        val strokeWidth = Math.max(12.0, Math.min(width, height) / 10)
        val radius = (Math.min(width, height) - strokeWidth) / 2
        var angle = 0.0
        val timeline = Timeline()
        data.forEach {
            val arc = it.node as Arc
            if (!it.wasAnimated) {
                arc.startAngle = HALF_PI

                val length = TWO_PI * (it.value / total)
                val angleKeyValue = KeyValue(arc.startAngleProperty(), -angle + HALF_PI, Interpolator.EASE_OUT)
                val lengthKeyValue = KeyValue(arc.lengthProperty(), -length, Interpolator.EASE_OUT)
                timeline.keyFrames.add(KeyFrame(Duration.millis(1000.0), angleKeyValue, lengthKeyValue))

                angle += length
                it.wasAnimated = true
            }
            arc.strokeWidth = strokeWidth
            arc.centerX = left + width / 2
            arc.centerY = top + height / 2
            arc.radiusX = radius
            arc.radiusY = radius
        }
        if (timeline.keyFrames.isNotEmpty()) timeline.play()
    }

    class Data(val name: String, val value: Long) {

        var node: Arc? = null
        var wasAnimated = false

        fun createNode(index: Int) {
            node = Arc().apply {
                addClass("donut-shape", "default-color${index % COLOR_COUNT}")
                type = ArcType.OPEN
            }
        }

    }

}
