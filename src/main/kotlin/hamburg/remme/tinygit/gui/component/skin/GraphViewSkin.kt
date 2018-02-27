package hamburg.remme.tinygit.gui.component.skin

import hamburg.remme.tinygit.domain.Graph
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.component.GraphView
import javafx.scene.Group
import javafx.scene.shape.Circle
import javafx.scene.shape.LineTo
import javafx.scene.shape.MoveTo
import javafx.scene.shape.Path
import javafx.scene.shape.QuadCurveTo

class GraphViewSkin(private val control: GraphView) : GraphViewSkinBase(control) {

    private val paths = Group()

    init {
        paths.isManaged = false
        children += paths
    }

    // TODO: inefficient and buggy; needs to be fully implemented
    override fun layoutGraphChildren() {
//        val graph = Graph.of(control.items)
//        paths.children.clear()
//        if (flow.firstVisibleCell != null && flow.lastVisibleCell != null) {
//            val graphPath = Path().addClass("commit-path")
//            paths.children += graphPath
//            (flow.firstVisibleCell.index..flow.lastVisibleCell.index).mapNotNull { flow.getVisibleCell(it) }
//                    .forEach { cell ->
//                        val node = graph[cell.index]
//
//                        val nx = 16.0 + 16.0 * node.tag
//                        val ny = cell.layoutY + cell.height / 2
//
//                        node.parents.forEach {
//                            val parentIndex = graph.indexOf(it)
//
//                            val tx = 16.0 + 16.0 * it.tag
//                            val ty = if (parentIndex > flow.lastVisibleCell.index) {
//                                parentIndex * cell.height
//                            } else {
//                                val target = flow.getCell(parentIndex)
//                                target.layoutY + target.height / 2
//                            }
//
//                            graphPath.elements += MoveTo(nx, ny)
//                            if (it.tag == node.tag) {
//                                graphPath.elements += LineTo(tx, ty)
//                            } else {
//                                graphPath.elements += QuadCurveTo(nx, ty, tx, ty)
//                            }
//                        }
//                        paths.children += Circle(nx, ny, 6.0).addClass("commit-node")
//                    }
//            control.graphWidth = 16.0 + 16.0 * graph.groupBy { it.tag }.count()
//        }
    }

}
