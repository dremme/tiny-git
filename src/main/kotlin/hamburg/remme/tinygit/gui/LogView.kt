package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.application.Platform
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.util.Callback
import org.eclipse.jgit.api.errors.TransportException

class LogView : Tab() {

    private val progressPane: ProgressPane
    private val localCommits = TableView<LocalCommit>()
    private val commitDetails = CommitDetailsView()
    private lateinit var head: String
    private var task: Task<*>? = null

    init {
        text = "Commits"
        graphic = FontAwesome.list()
        isClosable = false

        val message = tableColumn<LocalCommit, LocalCommit>("Message",
                cellValue = Callback { ReadOnlyObjectWrapper(it.value) },
                cellFactory = Callback { LogMessageTableCell() })
        val date = tableColumn<LocalCommit, String>("Date",
                cellValue = Callback { ReadOnlyStringWrapper(it.value.date.format(shortDate)) })
        val author = tableColumn<LocalCommit, String>("Author",
                cellValue = Callback { ReadOnlyStringWrapper(it.value.author) })
        val commit = tableColumn<LocalCommit, String>("Commit",
                cellValue = Callback { ReadOnlyStringWrapper(it.value.shortId) })

        localCommits.columns.addAll(message, date, author, commit)
        localCommits.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        localCommits.selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { commitDetails.update(State.getSelectedRepository(), it) }
        }

        val pane = SplitPane()
        pane.styleClass += "log-view"
        pane.items.addAll(localCommits, commitDetails)
        VBox.setVgrow(pane, Priority.ALWAYS)

        progressPane = ProgressPane(pane)
        content = progressPane

        State.selectedRepositoryProperty().addListener { _, _, it -> it?.let { logQuick(it) } }
        State.addRefreshListener { State.getSelectedRepository { logQuick(it) } }

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
        State.ahead.set(divergence.ahead)
        State.behind.set(divergence.behind)
    }

    private fun logQuick(repository: LocalRepository) {
        head = LocalGit.head(repository)
        updateLog(LocalGit.log(repository))
        updateDivergence(LocalGit.divergence(repository))
        logRemote(repository)
    }

    private fun logRemote(repository: LocalRepository) {
        task?.cancel()
        if (!LocalGit.isUpdated(repository)) {
            println("Fetching: $repository")
            task = object : Task<List<LocalCommit>>() {
                override fun call() = LocalGit.log(repository, true)

                override fun succeeded() {
                    updateLog(value)
                    updateDivergence(LocalGit.divergence(repository))
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
                    HBox(4.0, *item.branches.map { BranchBadge(it.shortRef) }.toTypedArray())
                } else null
            }
        }

    }

    private inner class BranchBadge(name: String) : Label(name, FontAwesome.codeFork()) {

        init {
            styleClass += "branch-badge"
            if (name == this@LogView.head) styleClass += "current"
        }

    }

}
