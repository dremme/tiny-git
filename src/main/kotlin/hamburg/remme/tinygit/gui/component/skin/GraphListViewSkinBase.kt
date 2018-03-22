package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.application.Platform
import javafx.scene.control.IndexedCell
import javafx.scene.control.ScrollBar
import javafx.scene.control.skin.ListViewSkin
import javafx.scene.control.skin.VirtualFlow

abstract class GraphListViewSkinBase(control: GraphListView) : ListViewSkin<Commit>(control) {

    @Suppress("UNCHECKED_CAST")
    protected val flow = children[0] as VirtualFlow<IndexedCell<Commit>>
    protected val horizontalBar = flow.childrenUnmodifiable[3] as ScrollBar
    protected val verticalBar = flow.childrenUnmodifiable[2] as ScrollBar
    protected val hasCells get() = flow.firstVisibleCell != null && flow.lastVisibleCell != null
    protected val firstCell get() = flow.firstVisibleCell!!
    protected val lastCell get() = flow.lastVisibleCell!!

    abstract fun layoutGraphChildren()

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        Platform.runLater { layoutGraphChildren() }
    }

}
