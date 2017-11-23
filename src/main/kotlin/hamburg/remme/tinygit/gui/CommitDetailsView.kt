package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.scene.control.SplitPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import javafx.scene.web.WebView

class CommitDetailsView : SplitPane() {

    private val files = FileStatusView().also { VBox.setVgrow(it, Priority.ALWAYS) }
    private val webView = WebView().also { it.isContextMenuEnabled = false }
    private var repository: LocalRepository? = null
    private var commit: LocalCommit? = null
    private var task: Task<*>? = null

    init {
        addClass("commit-details-view")

        val fileDiff = FileDiffView()
        files.selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { fileDiff.update(repository!!, it, commit!!.id) } ?: fileDiff.clear()
        }

        val info = StackPane(HBox(Text("This commit has no changes."))
                .addClass("box"))
                .addClass("overlay")
        info.visibleProperty().bind(Bindings.isEmpty(files.items))

        items.addAll(SplitPane(webView, StackPane(VBox(ToolBar(StatusCountView(files)), files), info)), fileDiff)
        clearContent()
    }

    fun update(newRepository: LocalRepository, newCommit: LocalCommit) {
        if (newRepository != repository || newCommit != commit) {
            repository = newRepository
            commit = newCommit

            setContent(newCommit)

            // TODO: add a process indicator
            println("Status for commit: ${newCommit.shortId}")
            task?.cancel()
            task = object : Task<List<LocalFile>>() {
                override fun call() = LocalGit.diffTree(newRepository, newCommit.id)

                override fun succeeded() {
                    files.items.setAll(value)
                }
            }.also { State.execute(it) }
        }
    }

    private fun clearContent() {
        //language=HTML
        webView.engine.loadContent("""
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
        webView.engine.loadContent("""
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
                    <tr><td class="label">Parents:</td><td>${commit.parents.joinToString()}</td></tr>
                    <tr><td class="label">Author:</td><td>${commit.author.htmlEncode()}</td></tr>
                    <tr><td class="label">Date:</td><td>${commit.date.format(fullDate)}</td></tr>
                    <tr><td colspan="2"><br/>${commit.fullMessage.htmlEncodeAll().replace("\r?\n".toRegex(), "<br/>")}</td></tr>
                </table>
            </body>
            </html>
        """)
    }

}
