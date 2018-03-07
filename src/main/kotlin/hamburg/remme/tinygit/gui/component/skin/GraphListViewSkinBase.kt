package hamburg.remme.tinygit.gui.component.skin

import com.sun.javafx.scene.control.skin.ListViewSkin
import com.sun.javafx.scene.control.skin.VirtualFlow
import com.sun.javafx.scene.control.skin.VirtualScrollBar
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.gui.component.GraphListView
import javafx.application.Platform

abstract class GraphListViewSkinBase(control: GraphListView) : ListViewSkin<Commit>(control) {

    init {
        // Yuck
        val vbarField = VirtualFlow::class.java.getDeclaredField("vbar")
        vbarField.isAccessible = true
        val vbar = vbarField.get(flow) as VirtualScrollBar
        vbar.valueProperty().addListener { _ -> Platform.runLater { layoutGraphChildren() } }
    }

    abstract fun layoutGraphChildren()

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)
        Platform.runLater { layoutGraphChildren() }
    }

}
