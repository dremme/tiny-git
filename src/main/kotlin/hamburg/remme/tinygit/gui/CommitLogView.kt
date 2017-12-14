package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.SHORT_DATE
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.ProgressPaneBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.column
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.progressPane
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
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
    private val progressPane: ProgressPaneBuilder
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
            it?.let { commitDetails.update(State.getSelectedRepository(), it) }
        }
        // TODO: you have to scroll further than the end to make this fire
        localCommits.setOnScroll { if (it.deltaY < 0) logMore(State.getSelectedRepository()) }

        progressPane = progressPane {
            +splitPane {
                addClass("log-view")
                vgrow(Priority.ALWAYS)
                +localCommits
                +commitDetails
            }
            +stackPane {
                addClass("overlay")
                visibleWhen(Bindings.isEmpty(localCommits.items))
                +Text("There are no commits.")
            }
        }
        content = progressPane

        State.addRepositoryListener {
            it?.let {
                skip = 0
                logQuick(it)
                localCommits.scrollTo(0)
            } ?: clearContent()
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

    private fun setContent(commits: List<LocalCommit>) {
        val selected = localCommits.selectionModel.selectedItem
        localCommits.items.setAll(commits)
        localCommits.items.find { it == selected }?.let { localCommits.selectionModel.select(it) }
        localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst()
    }

    private fun clearContent() {
        task?.cancel()
        localCommits.items.clear()
    }

    private fun logQuick(repository: LocalRepository) {
        task?.cancel()
        head = Git.head(repository)
        try {
            invalidateCache(repository)
            setContent(Git.log(repository, 0, logSize + skip))
        } catch (ex: NoHeadException) {
            clearContent()
        }
        logRemote(repository)
    }

    private fun logMore(repository: LocalRepository) {
        if (localCommits.items.size < skipSize) return
        val commits = Git.log(repository, skip + skipSize, logSize)
        if (commits.isNotEmpty()) {
            skip += skipSize
            localCommits.items.addAll(commits)
            localCommits.scrollTo(skip - 1)
        }
    }

    private fun logRemote(repository: LocalRepository) {
        if (!Git.hasRemote(repository) || Git.isUpdated(repository)) return

        task = object : Task<List<LocalCommit>>() {
            override fun call() = Git.logFetch(repository, 0, logSize + skip)

            override fun succeeded() {
                invalidateCache(repository)
                setContent(value)
            }

            override fun failed() {
                when (exception) {
                    is TransportException -> errorAlert(window, "Cannot Fetch Remote",
                            "Please check the repository settings.\nCredentials or proxy settings may have changed.")
                    else -> exception.printStackTrace()
                }
            }
        }.also { progressPane.execute(it) }
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
