package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.column
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.vgrow
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import org.eclipse.jgit.api.errors.TransportException

class CommitLogView : Tab() {

    private val progressPane: ProgressPane
    private val localCommits = TableView<LocalCommit>()
    private val commitDetails = CommitDetailsView()
    private lateinit var head: String
    private var task: Task<*>? = null

    init {
        text = "Commits"
        graphic = FontAwesome.list()
        isClosable = false

        val message = column<LocalCommit, LocalCommit> {
            text = "Message"
            isSortable = false
            setCellValueFactory { ReadOnlyObjectWrapper(it.value) }
            setCellFactory { LogMessageTableCell() }
        }
        val date = column<LocalCommit, String> {
            text = "Date"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.date.format(shortDate)) }
        }
        val author = column<LocalCommit, String> {
            text = "Author"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.author) }
        }
        val commit = column<LocalCommit, String> {
            text = "Commit"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.shortId) }
        }

        localCommits.columns.addAll(message, date, author, commit)
        localCommits.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        localCommits.selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { commitDetails.update(State.selectedRepository, it) }
        }

        progressPane = ProgressPane(splitPane {
            addClass("log-view")
            vgrow(Priority.ALWAYS)
            +localCommits
            +commitDetails
        })
        content = progressPane

        State.addRepositoryListener { it?.let { logQuick(it) } }
        State.addRefreshListener { logQuick(it) }

        Platform.runLater {
            localCommits.resizeColumn(message, 0.6 * localCommits.width)
            localCommits.resizeColumn(date, -0.1 * localCommits.width)
            localCommits.resizeColumn(author, 0.3 * localCommits.width)
            localCommits.resizeColumn(commit, -0.1 * localCommits.width)
        }
    }

    private fun updateLog(commits: List<LocalCommit>) {
        val selected = localCommits.selectionModel.selectedItem
        localCommits.items.setAll(commits)
        localCommits.items.find { it == selected }?.let { localCommits.selectionModel.select(it) }
        localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst()
    }

    private fun updateDivergence(divergence: LocalDivergence) {
        State.ahead = divergence.ahead
        State.behind = divergence.behind
    }

    private fun logQuick(repository: LocalRepository) {
        head = Git.head(repository)
        updateLog(Git.log(repository))
        updateDivergence(Git.divergence(repository))
        logRemote(repository)
    }

    private fun logRemote(repository: LocalRepository) {
        task?.cancel()
        if (!Git.isUpdated(repository)) {
            println("Fetching: $repository")
            task = object : Task<List<LocalCommit>>() {
                override fun call() = Git.log(repository, true)

                override fun succeeded() {
                    updateLog(value)
                    updateDivergence(Git.divergence(repository))
                }

                override fun failed() {
                    when (exception) {
                        is TransportException -> errorAlert(content.scene.window,
                                "Cannot Fetch Remote",
                                "Please check the repository settings.\nCredentials or proxy settings may have changed.")
                        else -> exception.printStackTrace()
                    }
                }

                override fun done() {
                    Platform.runLater {
                        if (state != Worker.State.CANCELLED) progressPane.hideProgress()
                        else if (task?.state != Worker.State.READY) progressPane.hideProgress()
                    }
                }
            }.also {
                progressPane.showProgress()
                State.execute(it)
            }
        }
    }

    private inner class LogMessageTableCell : TableCell<LocalCommit, LocalCommit>() {

        override fun updateItem(item: LocalCommit?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortMessage
            graphic = if (empty) null else {
                if (item!!.branches.isNotEmpty()) {
                    hbox {
                        spacing = 4.0
                        item.branches.forEach { +BranchBadge(it.shortRef) }
                    }
                } else null
            }
        }

    }

    private inner class BranchBadge(name: String) : Label(name, FontAwesome.codeFork()) {

        init {
            addClass("branch-badge")
            if (name == this@CommitLogView.head) addClass("current")
        }

    }

}
