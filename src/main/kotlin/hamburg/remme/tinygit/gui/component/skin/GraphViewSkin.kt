package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.GraphView
import javafx.beans.property.ReadOnlyDoubleWrapper
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

class GraphViewSkin(control: GraphView) : GraphViewSkinBase(control) {

    private val graph = Group()
    private val graphWidth = ReadOnlyDoubleWrapper(32.0)

    init {
        graph.isManaged = false
        children += graph
    }

    override fun graphWidthProperty() = graphWidth.readOnlyProperty!!

    override fun getGraphWidth() = graphWidthProperty().get()

    // TODO: inefficient and buggy; needs to be fully implemented
    override fun layoutGraphChildren() {
        graph.children.clear()
        if (flow.firstVisibleCell != null && flow.lastVisibleCell != null) {
            val graphPath = Path().addClass("commit-path")
            graph.children += graphPath
            (flow.firstVisibleCell.index..flow.lastVisibleCell.index).mapNotNull { flow.getVisibleCell(it) }
                    .forEach {
                        if (it.item.parents.isNotEmpty()) {
                            graphPath.elements += MoveTo(16.0, it.layoutY + it.height / 2)
                            graphPath.elements += LineTo(16.0, it.layoutY + it.height / 2 + it.height)
                        }
                        graph.children += Circle(16.0, it.layoutY + it.height / 2, 6.0).addClass("commit-node")
                    }
            graphWidth.set(32.0)
        }
    }

}
