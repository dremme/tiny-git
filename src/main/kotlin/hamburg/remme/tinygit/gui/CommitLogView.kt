package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.gui.builder.ProgressPane
import hamburg.remme.tinygit.gui.builder.addClass
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
import javafx.collections.ListChangeListener
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.text.Text

class CommitLogView : Tab() {

    private val progressPane: ProgressPane
    private val localCommits = TableView<Commit>(CommitLogService.commits)
    private val selectedCommit: Commit?
        @Suppress("UNNECESSARY_SAFE_CALL") get() = localCommits?.selectionModel?.selectedItem
    private val commitDetails = CommitDetailsView()

    init {
        text = "Commits"
        graphic = Icons.list()
        isClosable = false

        val message = tableColumn<Commit, Commit> {
            text = "Message"
            isSortable = false
            setCellValueFactory { ReadOnlyObjectWrapper(it.value) }
            setCellFactory { LogMessageTableCell() }
        }
        val date = tableColumn<Commit, String> {
            text = "Date"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.date.format(shortDateTimeFormat)) }
        }
        val author = tableColumn<Commit, String> {
            text = "Author"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.author) }
        }
        val commit = tableColumn<Commit, String> {
            text = "Commit"
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(it.value.shortId) }
        }

        localCommits.items.addListener(ListChangeListener { selectedCommit ?: localCommits.selectionModel.selectFirst() })
        localCommits.columns.addAll(message, date, author, commit)
        localCommits.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        localCommits.selectionModel.selectedItemProperty().addListener { _, _, it -> CommitLogService.activeCommit.set(it) }
        localCommits.setOnScroll {
            if (it.deltaY < 0) {
                CommitLogService.logMore()
                localCommits.scrollTo(selectedCommit)
            }
        }
        localCommits.setOnKeyPressed {
            if (it.code == KeyCode.DOWN && selectedCommit == localCommits.items.last()) {
                CommitLogService.logMore()
                localCommits.scrollTo(selectedCommit)
            }
        }

        // TODO: progress pane not working atm
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

        Platform.runLater {
            localCommits.resizeColumn(message, localCommits.width * 0.6)
            localCommits.resizeColumn(date, localCommits.width * -0.1)
            localCommits.resizeColumn(author, localCommits.width * 0.3)
            localCommits.resizeColumn(commit, localCommits.width * -0.1)
        }
    }

    private inner class LogMessageTableCell : TableCell<Commit, Commit>() {

        // TODO: does not refresh when head changes
        override fun updateItem(item: Commit?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortMessage
            graphic = if (empty || item!!.refs.isEmpty()) null else hbox {
                spacing = 4.0
                item.refs.forEach {
                    +label {
                        addClass("branch-badge")
                        if (it == BranchService.head.get()) addClass("current")
                        text = it.substringAfter("tag:").trim()
                        graphic = if (it.startsWith("tag:")) Icons.tag() else Icons.codeFork()
                    }
                }
            }
        }

    }

}
