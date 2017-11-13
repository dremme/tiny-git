package hamburg.remme.tinygit.gui

import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ProgressBar
import javafx.scene.layout.StackPane

// TODO: move task handling here aswell?
class ProgressPane(vararg node: Node) : StackPane(*node) {

    private val progress = ProgressBar(-1.0)

    init {
        progress.styleClass += "log-progress"
        progress.maxWidth = Double.MAX_VALUE
        StackPane.setAlignment(progress, Pos.TOP_CENTER)

        children += progress
    }

    fun showProgress() {
        progress.isVisible = true
    }

    fun hideProgress() {
        progress.isVisible = false
    }

}
