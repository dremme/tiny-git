package hamburg.remme.tinygit.gui.component

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.label
import javafx.scene.layout.Pane
import javafx.scene.layout.Region

private const val DEFAULT_STYLE_CLASS = "chart"
private const val TITLE_STYLE_CLASS = "title"

abstract class Chart(title: String) : Region() {

    private val titleLabel = label {
        addClass(TITLE_STYLE_CLASS)
        text = title
    }
    private val chartContent = object : Pane() {
        override fun layoutChildren() {
            layoutChartChildren(snapSizeX(width), snapSizeY(height))
        }
    }
    protected val chartChildren get() = chartContent.children!!

    init {
        addClass(DEFAULT_STYLE_CLASS)
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
        top += titleHeight + titleLabel.insets.top + titleLabel.insets.bottom // TODO: with insets?

        chartContent.resizeRelocate(left, top, width - left - right, height - top - bottom)
    }

    protected fun requestChartLayout() = chartContent.requestLayout()

    abstract fun layoutChartChildren(width: Double, height: Double)

}
