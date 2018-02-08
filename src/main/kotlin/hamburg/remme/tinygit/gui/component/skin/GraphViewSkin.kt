package hamburg.remme.tinygit.gui.component.skin

import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.sun.javafx.scene.control.skin.VirtualScrollBar
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.GraphView
import javafx.application.Platform
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path

class GraphViewSkin(control: GraphView) : ListViewSkin<Commit>(control) {

    val padding = SimpleDoubleProperty(32.0)
    private val graph = Group()
    private val vbar: VirtualScrollBar

    init {
        graph.isManaged = false
        children += graph

        // Yuck
        val vbarField = VirtualFlow::class.java.getDeclaredField("vbar")
        vbarField.isAccessible = true
        vbar = vbarField.get(flow) as VirtualScrollBar
        vbar.valueProperty().addListener { _ -> Platform.runLater { layoutGraph() } }
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        Platform.runLater { layoutGraph() }
    }

    // TODO: inefficient and buggy; needs to be fully implemented
    private fun layoutGraph() {
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
            padding.set(32.0)
        }
    }

}
