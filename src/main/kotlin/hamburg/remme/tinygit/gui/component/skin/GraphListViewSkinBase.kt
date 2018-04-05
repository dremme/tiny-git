package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.application.Platform
import javafx.geometry.Orientation
import javafx.scene.control.IndexedCell
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ListViewSkin
import javafx.scene.control.skin.VirtualFlow

/**
 * A class for enhancing the [ListViewSkin] with some private fields.
 */
abstract class GraphListViewSkinBase(control: GraphListView) : ListViewSkin<Commit>(control) {

    @Suppress("UNCHECKED_CAST")
    protected val flow = control.lookup("#virtual-flow") as VirtualFlow<IndexedCell<Commit>>
    protected val horizontalBar = lookupScrollBar(Orientation.HORIZONTAL)
    protected val verticalBar = lookupScrollBar(Orientation.VERTICAL)
    protected val hasCells get() = flow.firstVisibleCell != null && flow.lastVisibleCell != null
    protected val firstCell get() = flow.firstVisibleCell!!
    protected val lastCell get() = flow.lastVisibleCell!!

    /**
     * The list rendering is handled by the [ListViewSkin] but the graph has to be rendered here.
     */
    abstract fun layoutGraphChildren()

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        Platform.runLater { layoutGraphChildren() }
    }

    private fun lookupScrollBar(orientation: Orientation): ScrollBar {
        return flow.lookupAll(".scroll-bar")
                .map { it as ScrollBar }
                .find { it.orientation == orientation }!!
    }

}
