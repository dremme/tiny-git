package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

abstract class Chart(val title: String) : Region() {

    private val titleLabel = label {
        addClass("diagram-title")
        +title
    }
    private val chartContent = object : Pane() {
        override fun layoutChildren() {
            layoutChartChildren(snapSizeX(width), snapSizeY(height))
        }
    }
    protected val chartChildren get() = chartContent.children!!

    init {
        addClass("diagram") // chart is taken by modena.css
        chartContent.isManaged = false
        children.addAll(titleLabel, chartContent)
    }

    override fun layoutChildren() {
        var top = snappedTopInset()
        val left = snappedLeftInset()
        val bottom = snappedBottomInset()
        val right = snappedRightInset()

        val titleHeight = snapSizeY(titleLabel.prefHeight(width - left - right))
        titleLabel.resizeRelocate(left, top, width - left - right, titleHeight)
        top += titleHeight

        chartContent.resizeRelocate(left, top, width - left - right, height - top - bottom)
    }

    protected fun requestChartLayout() = chartContent.requestLayout()

    abstract fun layoutChartChildren(width: Double, height: Double)

}
