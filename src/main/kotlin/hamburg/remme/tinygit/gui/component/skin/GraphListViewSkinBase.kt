package hamburg.remme.tinygit.gui.component.skin

import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.VirtualScrollBar
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.getReflective
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.application.Platform

abstract class GraphListViewSkinBase(control: GraphListView) : ListViewSkin<Commit>(control) {

    // TODO: hbar clipping through the graph
    private val vertical = flow.getReflective<VirtualScrollBar>("vbar")!!
    private val horizontal = flow.getReflective<VirtualScrollBar>("hbar")!!

    init {
        horizontal.valueProperty().addListener { _, _, it -> Platform.runLater { layoutGraphChildren(it.toDouble(), vertical.value) } }
        vertical.valueProperty().addListener { _, _, it -> Platform.runLater { layoutGraphChildren(horizontal.value, it.toDouble()) } }
    }

    abstract fun layoutGraphChildren(scrollX: Double, scrollY: Double)

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        Platform.runLater { layoutGraphChildren(horizontal.value, vertical.value) }
    }

}
