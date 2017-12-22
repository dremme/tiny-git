package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.dateTimeFormat
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.SplitPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.builder.webView
import hamburg.remme.tinygit.htmlEncode
import hamburg.remme.tinygit.htmlEncodeAll
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.scene.web.WebEngine

class CommitDetailsView : SplitPaneBuilder() {

    private val files: FileStatusView
    private val details: WebEngine
    private var repository: LocalRepository? = null
    private var commit: LocalCommit? = null
    private var task: Task<*>? = null

    init {
        addClass("commit-details-view")

        val fileDiff = FileDiffView()
        files = FileStatusView()
        files.vgrow(Priority.ALWAYS)
        files.selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { fileDiff.update(repository!!, it, commit!!) } ?: fileDiff.clearContent()
        }

        val webView = webView { isContextMenuEnabled = false }
        details = webView.engine

        +splitPane {
            +webView
            +stackPane {
                +vbox {
                    +toolBar { +StatusCountView(files) }
                    +files
                }
                +stackPane {
                    addClass("overlay")
                    visibleWhen(Bindings.isEmpty(files.items))
                    +Text("This commit has no changes.")
                }
            }
        }
        +fileDiff
        clearContent()
    }

    fun update(newRepository: LocalRepository, newCommit: LocalCommit) {
        if (newRepository != repository || newCommit != commit) {
            repository = newRepository
            commit = newCommit

            setContent(newCommit)

            // TODO: add a process indicator
            task?.cancel()
            task = object : Task<List<LocalFile>>() {
                override fun call() = Git.diffTree(newRepository, newCommit)

                override fun succeeded() {
                    files.items.setAll(value)
                }

                override fun failed() = exception.printStackTrace()
            }.also { State.execute(it) }
        }
    }

    private fun clearContent() {
        repository = null
        commit = null

        //language=HTML
        details.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        background-color: #3c3f41;
                    }
                </style>
            </head>
            </html>
        """)
    }

    private fun setContent(commit: LocalCommit) {
        //language=HTML
        details.loadContent("""
            <html>
            <head>
                <style>
                    html, body {
                        padding: 0;
                        margin: 0;
                        width: 100%;
                        height: 100%;
                        font: 13px "Roboto", sans-serif;
                        color: white;
                        background-color: #3c3f41;
                    }
                    table {
                        padding: 8px;
                        position: absolute;
                        min-width: 100%;
                        font-size: 13px;
                    }
                    .label {
                        font-weight: bold;
                    }
                </style>
            </head>
            <body>
                <table>
                    <tr><td class="label">Commit:</td><td>${commit.id} [${commit.shortId}]</td></tr>
                    <tr><td class="label">Parents:</td><td>${commit.shortParents.joinToString()}</td></tr>
                    <tr><td class="label">Author:</td><td>${commit.author.htmlEncode()}</td></tr>
                    <tr><td class="label">Date:</td><td>${commit.date.format(dateTimeFormat)}</td></tr>
                    <tr><td colspan="2"><br/>${commit.fullMessage.htmlEncodeAll().replace("\r?\n".toRegex(), "<br/>")}</td></tr>
                </table>
            </body>
            </html>
        """)
    }

}
