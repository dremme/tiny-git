package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalGraph
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.ProgressPane
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.progressPane
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.tableColumn
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.shortDateTimeFormat
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
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
    private var task: Task<*>? = null

    private val cache: MutableMap<String, List<LocalBranch>> = mutableMapOf()
    private val logSize = 50
    private val skipSize = 50
    private lateinit var graph: LocalGraph
    private lateinit var head: String
    private var skip = 0

    init {
        text = "Commits"
        graphic = Icons.list()
        isClosable = false

        val message = tableColumn<LocalCommit, LocalCommit> {
            text = "Message"
            isSortable = false
            setCellValueFactory { ReadOnlyObjectWrapper(it.value) }
            setCellFactory { LogMessageTableCell() }
        }
        val date = tableColumn<LocalCommit, String> {
            text = "Date"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.date.format(shortDateTimeFormat)) }
        }
        val author = tableColumn<LocalCommit, String> {
            text = "Author"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.author) }
        }
        val commit = tableColumn<LocalCommit, String> {
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
            clearContent()
            it?.let {
                skip = 0
                logQuick(it)
                localCommits.scrollTo(0)
            }
        }
        State.addRefreshListener(this) { logQuick(it) }

        Platform.runLater {
            localCommits.resizeColumn(message, localCommits.width * 0.6)
            localCommits.resizeColumn(date, localCommits.width * -0.1)
            localCommits.resizeColumn(author, localCommits.width * 0.3)
            localCommits.resizeColumn(commit, localCommits.width * -0.1)
        }
    }

    private fun invalidateCache(repository: LocalRepository) {
        cache.clear()
        cache.putAll(Git.branchListAll(repository).groupBy { it.commitId })
    }

    private fun setContent(commits: List<LocalCommit>) {
        graph = LocalGraph(commits)
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
        invalidateCache(repository)
        try {
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
            graph = LocalGraph(localCommits.items + commits) // TODO: add-function on graph
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
                State.fireRefresh(this)
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
                        it.forEach {
                            +label {
                                addClass("branch-badge")
                                if (it.shortRef == head) addClass("current")
                                text = it.shortRef
                                graphic = Icons.codeFork()
                            }
                        }
                    }
                }
            }
        }

    }

}
