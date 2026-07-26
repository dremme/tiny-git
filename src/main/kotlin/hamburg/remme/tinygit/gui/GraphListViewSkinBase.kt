package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.Commit
import javafx.geometry.Orientation
import javafx.scene.control.IndexedCell
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ListViewSkin
import javafx.scene.control.skin.VirtualFlow

/**
 * [ListViewSkin] with access to the virtual flow / scroll bars for graph overlay layout.
 */
abstract class GraphListViewSkinBase(
    control: GraphListView,
) : ListViewSkin<Commit>(control) {
    @Suppress("UNCHECKED_CAST")
    protected val flow = control.lookup("#virtual-flow") as VirtualFlow<IndexedCell<Commit>>
    protected val horizontalBar = lookupScrollBar(Orientation.HORIZONTAL)
    protected val verticalBar = lookupScrollBar(Orientation.VERTICAL)
    protected val hasCells get() = flow.firstVisibleCell != null && flow.lastVisibleCell != null
    protected val firstCell get() = flow.firstVisibleCell!!
    protected val lastCell get() = flow.lastVisibleCell!!

    /** Draw the commit graph over the list cells. */
    abstract fun layoutGraphChildren()

    override fun layoutChildren(
        x: Double,
        y: Double,
        w: Double,
        h: Double,
    ) {
        super.layoutChildren(x, y, w, h)
        // Same pulse as cells — deferring this one frame caused graph flicker on selection.
        layoutGraphChildren()
    }

    private fun lookupScrollBar(orientation: Orientation): ScrollBar =
        flow
            .lookupAll(".scroll-bar")
            .map { it as ScrollBar }
            .find { it.orientation == orientation }!!
}
