package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.concurrent.Task
import javafx.geometry.Orientation
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class CommitDetailsView : SplitPane() {

    private val files = FileStatusView().also { VBox.setVgrow(it, Priority.ALWAYS) }
    private val commitId = textField("", false)
    private val parents = textField("", false)
    private val author = textField("", false)
    private val date = textField("", false)
    private val message = textArea("", editable = false)
    private var repository: LocalRepository? = null
    private var commit: LocalCommit? = null
    private var task: Task<*>? = null

    init {
        styleClass += "commit-details-view"

        val form = GridPane()
        form.styleClass += "commit-details"
        form.add(Label("Commit:"), 0, 0)
        form.add(commitId, 1, 0)
        form.add(Label("Parents:"), 0, 1)
        form.add(parents, 1, 1)
        form.add(Label("Author:"), 0, 2)
        form.add(author, 1, 2)
        form.add(Label("Date:"), 0, 3)
        form.add(date, 1, 3)
        form.add(Label(" "), 0, 4)
        form.add(message, 0, 5, 2, 1)

        val fileDiff = FileDiffView()
        files.selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { fileDiff.update(repository!!, files.selectionModel.selectedItem, commit!!.id) } ?: fileDiff.clear()
        }

        val pane = SplitPane(ScrollPane(form), VBox(ToolBar(StatusCountView(files)), files))
        pane.orientation = Orientation.VERTICAL

        items.addAll(pane, fileDiff)
    }

    fun update(repository: LocalRepository, commit: LocalCommit) {
        if (repository != this.repository || commit != this.commit) {
            this.repository = repository
            this.commit = commit

            commitId.text = "${commit.id} [${commit.shortId}]"
            parents.text = commit.parents.joinToString()
            author.text = commit.author
            date.text = commit.date.format(fullDate)
            message.text = commit.fullMessage

            println("Status for commit: ${commit.shortId}")
            task?.cancel()
            task = object : Task<List<LocalFile>>() {
                override fun call() = LocalGit.diffTree(repository, commit.id)

                override fun succeeded() {
                    files.items.setAll(value)
                }
            }
            State.cachedThreadPool.execute(task)
        }
    }

    fun clear() {
        repository = null
        commit = null

        commitId.text = ""
        parents.text = ""
        author.text = ""
        date.text = ""
        message.text = ""

        files.items.clear()
    }

}
