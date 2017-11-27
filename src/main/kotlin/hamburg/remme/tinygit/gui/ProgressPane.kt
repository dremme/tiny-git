package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.gui.builder.StackPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.alignment
import hamburg.remme.tinygit.gui.builder.progressBar
import javafx.geometry.Pos
import javafx.scene.Node

// TODO: move task handling here aswell?
class ProgressPane(vararg node: Node) : StackPaneBuilder() {

    private val progress = progressBar {
        addClass("log-progress")
        alignment(Pos.TOP_CENTER)
        maxWidth = Double.MAX_VALUE
        isVisible = false
    }

    init {
        node.forEach { +it }
        +progress
    }

    fun showProgress() {
        progress.isVisible = true
    }

    fun hideProgress() {
        progress.isVisible = false
    }

}
