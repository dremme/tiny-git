package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.service.DiffService
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.webView
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.value.ObservableObjectValue
import javafx.scene.control.ListCell
import javafx.scene.layout.Priority
import javafx.scene.web.WebEngine
import javafx.util.Callback

class FileDiffView(private val file: ObservableObjectValue<File?>,
                   private val commit: ObservableObjectValue<Commit?> = ReadOnlyObjectWrapper()) : VBoxBuilder() {

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
    private val fileDiff: WebEngine

    init {
        val contextLines = comboBox<Int> {
            items.addAll(0, 1, 3, 6, 12, 25, 50, 100)
            buttonCell = ContextLinesListCell()
            cellFactory = Callback { ContextLinesListCell() }
            value = 3
            valueProperty().addListener { _, _, it -> update(it) }
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
            engine.loadContent(empty)
        }
        fileDiff = webView.engine
        +webView

        file.addListener { _, _, _ -> update(contextLines.value) }
        State.addRefreshListener { update(contextLines.value) }
    }

    private fun update(contextLines: Int) {
        if (file.get() != null) {
            if (commit.get() != null) fileDiff.loadContent(DiffService.diff(file.get()!!, commit.get()!!, contextLines))
            else fileDiff.loadContent(DiffService.diff(file.get()!!, contextLines))
        } else {
            fileDiff.loadContent(empty)
        }
    }

    private class ContextLinesListCell : ListCell<Int>() {
        override fun updateItem(item: Int?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.let { "$it lines" }
        }
    }

}
