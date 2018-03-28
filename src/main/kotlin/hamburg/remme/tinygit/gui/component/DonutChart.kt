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

private const val DEFAULT_STYLE_CLASS = "donut"
private const val VALUE_STYLE_CLASS = "value"
private const val DESCR_STYLE_CLASS = "description"
private const val SHAPE_STYLE_CLASS = "shape"
private const val ARC_STYLE_CLASS = "arc-color"
private const val COLOR_COUNT = 16

class DonutChart(title: String) : Chart(title) {

    private val data = mutableListOf<Data>()
    private val arcs get() = data.map { it.node }
    private val valueLabel = label { addClass(VALUE_STYLE_CLASS) }
    private val descriptionLabel = label { addClass(DESCR_STYLE_CLASS) }
    private var total = 0.0

    init {
        addClass(DEFAULT_STYLE_CLASS)
        chartChildren.addAll(valueLabel, descriptionLabel)
    }

    fun setData(data: List<Data>, description: (Long) -> String) {
        // Remove old arcs first
        chartChildren -= arcs
        // Set new reference list
        this.data.clear()
        this.data += data.takeLast(COLOR_COUNT)
        this.data.forEachIndexed { i, it -> it.createNode(i) }
        // Set sum of values
        total = this.data.map { it.value }.sum().toDouble()
        // Set labels with original data
        val sum = data.map { it.value }.sum() // not this.data!
        valueLabel.text = sum.toString()
        descriptionLabel.text = "...${description(sum)}"
        // Add new arcs
        chartChildren += arcs
        // Finally request chart layout
        requestChartLayout()
    }

    override fun layoutChartChildren(width: Double, height: Double) {
        val valueHeight = snapSizeY(valueLabel.prefHeight(width))
        valueLabel.resizeRelocate(0.0, height / 2 - valueHeight / 2, width, valueHeight)

        val descriptionHeight = snapSizeY(descriptionLabel.prefHeight(width))
        descriptionLabel.resizeRelocate(0.0, height / 2 + valueHeight / 2, width, descriptionHeight)

        val strokeWidth = Math.max(12.0, Math.min(width, height) / 10)
        val radius = (Math.min(width, height) - strokeWidth) / 2
        var angle = 0.0
        val timeline = Timeline()
        data.forEach {
            val arc = it.node as Arc
            if (!it.wasAnimated) {
                arc.startAngle = HALF_PI

                val length = TWO_PI * (it.value / total)
                timeline.keyFrames += KeyFrame(Duration.millis(1000.0),
                        KeyValue(arc.startAngleProperty(), -angle + HALF_PI, Interpolator.EASE_OUT),
                        KeyValue(arc.lengthProperty(), -length, Interpolator.EASE_OUT))

                angle += length
                it.wasAnimated = true
            }
            arc.strokeWidth = strokeWidth
            arc.centerX = width / 2
            arc.centerY = height / 2
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
                addClass(SHAPE_STYLE_CLASS, "$ARC_STYLE_CLASS${index % COLOR_COUNT}")
                type = ArcType.OPEN
            }
        }

    }

}
