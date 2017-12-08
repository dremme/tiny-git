package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.SHORT_DATE
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.column
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
import javafx.concurrent.Worker
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Window
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.api.errors.TransportException

class CommitLogView : Tab() {

    private val window: Window get() = content.scene.window
    private val progressPane: ProgressPane
    private val localCommits = TableView<LocalCommit>()
    private val commitDetails = CommitDetailsView()
    private val cache: MutableMap<String, List<LocalBranch>> = mutableMapOf()
    private lateinit var head: String
    private var task: Task<*>? = null

    private val logSize = 50
    private val skipSize = 50
    private var skip = 0

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
            setCellValueFactory { ReadOnlyStringWrapper(it.value.date.format(SHORT_DATE)) }
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
        // TODO: you have to scroll further than the end to make this fire
        localCommits.setOnScroll { if (it.deltaY < 0) logMore(State.selectedRepository) }

        progressPane = ProgressPane(
                splitPane {
                    addClass("log-view")
                    vgrow(Priority.ALWAYS)
                    +localCommits
                    +commitDetails
                },
                stackPane {
                    addClass("overlay")
                    visibleWhen(Bindings.isEmpty(localCommits.items))
                    +Text("There are no commits.")
                })
        content = progressPane

        State.addRepositoryListener {
            skip = 0
            logQuick(it)
            localCommits.scrollTo(0)
        }
        State.addRefreshListener { logQuick(it) }

        Platform.runLater {
            localCommits.resizeColumn(message, 0.6 * localCommits.width)
            localCommits.resizeColumn(date, -0.1 * localCommits.width)
            localCommits.resizeColumn(author, 0.3 * localCommits.width)
            localCommits.resizeColumn(commit, -0.1 * localCommits.width)
        }
    }

    private fun invalidateCache(repository: LocalRepository) {
        cache.clear()
        cache.putAll(Git.branchListAll(repository).groupBy { it.commitId })
    }

    private fun clearLog() {
        localCommits.items.clear()
    }

    private fun updateLog(commits: List<LocalCommit>) {
        val selected = localCommits.selectionModel.selectedItem
        localCommits.items.setAll(commits)
        localCommits.items.find { it == selected }?.let { localCommits.selectionModel.select(it) }
        localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst()
    }

    private fun addLog(commits: List<LocalCommit>) {
        localCommits.items.addAll(commits)
        localCommits.scrollTo(skip - 1)
    }

    private fun clearDivergence() {
        State.ahead = 0
        State.behind = 0
    }

    private fun updateDivergence(divergence: LocalDivergence) {
        State.ahead = divergence.ahead
        State.behind = divergence.behind
    }

    private fun logQuick(repository: LocalRepository) {
        head = Git.head(repository)
        try {
            invalidateCache(repository)
            updateLog(Git.log(repository, 0, logSize + skip))
            updateDivergence(Git.divergence(repository))
        } catch (ex: NoHeadException) {
            clearLog()
            clearDivergence()
        }
        logRemote(repository)
    }

    private fun logMore(repository: LocalRepository) {
        if (localCommits.items.size < skipSize) return
        val commits = Git.log(repository, skip + skipSize, logSize)
        if (commits.isNotEmpty()) {
            skip += skipSize
            addLog(commits)
        }
    }

    private fun logRemote(repository: LocalRepository) {
        task?.cancel()

        if (!Git.hasRemote(repository) || Git.isUpdated(repository)) return

        task = object : Task<List<LocalCommit>>() {
            override fun call() = Git.logFetch(repository, 0, logSize + skip)

            override fun succeeded() {
                invalidateCache(repository)
                updateLog(value)
                updateDivergence(Git.divergence(repository))
            }

            override fun failed() {
                when (exception) {
                    is TransportException -> errorAlert(window, "Cannot Fetch Remote",
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

    private inner class LogMessageTableCell : TableCell<LocalCommit, LocalCommit>() {

        override fun updateItem(item: LocalCommit?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortMessage
            graphic = if (empty) null else {
                cache[item!!.id]?.let {
                    hbox {
                        spacing = 4.0
                        it.forEach { +BranchBadge(it.shortRef) }
                    }
                }
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
