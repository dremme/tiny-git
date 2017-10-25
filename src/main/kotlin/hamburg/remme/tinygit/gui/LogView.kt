package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.util.Callback
import org.kordamp.ikonli.fontawesome.FontAwesome

class LogView : Tab() {

    private val error = StackPane()
    private val overlay = StackPane(ProgressIndicator(-1.0))
    private val localCommits = TableView<LocalCommit>()
    private val commitDetails = CommitDetailsView()
    private var task: Task<*>? = null

    init {
        text = "Log"
        graphic = icon(FontAwesome.LIST)
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

        error.children += HBox(
                Label("Fetching repository failed. Check the settings. "),
                Label("", icon(FontAwesome.COG)))
                .also { it.styleClass += "box" }
        error.styleClass += "overlay"
        error.isVisible = false

        overlay.styleClass += "progress-overlay"

        content = StackPane(pane, error, overlay)

        State.selectedRepositoryProperty().addListener { _, _, it -> fetchCommits(it) }
        State.addRefreshListener { fetchCurrent() }
    }

    private fun fetchCurrent() {
        if (State.hasSelectedRepository()) fetchCommits(State.getSelectedRepository())
    }

    private fun fetchCommits(repository: LocalRepository) {
        println("Fetching: $repository")
        task?.cancel()
        task = object : Task<List<LocalCommit>>() {
            val selected = localCommits.selectionModel.selectedItem

            override fun call() = LocalGit.log(repository)

            override fun succeeded() {
                error.isVisible = false
                overlay.isVisible = false
                localCommits.items.setAll(value)
                localCommits.items.find { it == selected }?.let { localCommits.selectionModel.select(it) }
                localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst()
            }

            override fun failed() {
                error.isVisible = true
                overlay.isVisible = false
                exception.printStackTrace()
            }
        }
        overlay.isVisible = true
        State.cachedThreadPool.execute(task)
    }

    private class LogMessageTableCell : TableCell<LocalCommit, LocalCommit>() {

        override fun updateItem(item: LocalCommit?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortMessage
            graphic = if (empty) null else {
                if (item!!.branches.isNotEmpty()) {
                    HBox(4.0, *item.branches.map { BranchBadge(it.shortRef, it.current) }.toTypedArray())
                } else null
            }
        }

    }

    private class BranchBadge(name: String, current: Boolean) : Label(name, icon(FontAwesome.CODE_FORK)) {

        init {
            styleClass += "branch-badge"
            if (current) styleClass += "current"
        }

    }

}
