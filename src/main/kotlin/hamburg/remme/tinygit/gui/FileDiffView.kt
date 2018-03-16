package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.webView
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableObjectValue
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import javafx.scene.web.WebEngine
import javafx.util.Callback

class FileDiffView(private val file: ObservableObjectValue<File?>,
                   private val commit: ObservableObjectValue<Commit?> = SimpleObjectProperty()) : VBoxBuilder() {

    //language=HTML
    private val empty = """
        <html>
        <head>
            <style>
                html, body {
                    background-color: #3c3f41;
                }
            </style>
        </head>
        </html>
    """
    private val diffService = TinyGit.diffService
    private val contextLines: ComboBox<Int>
    private val engine: WebEngine
    private var diff = empty

    init {
        contextLines = comboBox {
            items.addAll(0, 1, 3, 6, 12, 25, 50, 100)
            buttonCell = ContextLinesListCell()
            cellFactory = Callback { ContextLinesListCell() }
            value = 3
            valueProperty().addListener { _ -> update() }
        }
        +toolBar {
            addSpacer()
            +contextLines
        }

        val webView = webView {
            vgrow(Priority.ALWAYS)
            isContextMenuEnabled = false
            prefWidth = 400.0
            prefHeight = 300.0
            engine.loadContent(diff)
        }
        engine = webView.engine
        +webView

        file.addListener { _ -> update() }
    }

    fun refresh() = update()

    private fun update() {
        if (file.get() != null) {
            val newDiff = if (commit.get() != null) {
                diffService.diff(file.get()!!, commit.get()!!, contextLines.value)
            } else {
                diffService.diff(file.get()!!, contextLines.value)
            }
            if (diff != newDiff) {
                diff = newDiff
                engine.loadContent(diff)
            }
        } else {
            diff = empty
            engine.loadContent(diff)
        }
    }

    private class ContextLinesListCell : ListCell<Int>() {

        override fun updateItem(item: Int?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.let { I18N["fileDiff.lines", it] }
        }

    }

}
