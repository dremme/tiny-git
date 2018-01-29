package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
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
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.Tab
import javafx.scene.control.TableCell
import javafx.scene.control.TableView
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.text.Text

class CommitLogView : Tab() {

    private val logService = TinyGit.commitLogService
    private val branchService = TinyGit.branchService
    private val window get() = content.scene.window

    init {
        text = "Commits"
        graphic = Icons.list()
        isClosable = false

        val message = tableColumn<Commit, Commit> {
            text = "Message"
            isSortable = false
            setCellValueFactory { SimpleObjectProperty(it.value) }
            setCellFactory { LogMessageTableCell() }
        }
        val date = tableColumn<Commit, String> {
            text = "Date"
            isSortable = false
            setCellValueFactory { SimpleStringProperty(it.value.date.format(shortDateTimeFormat)) }
        }
        val author = tableColumn<Commit, String> {
            text = "Author"
            isSortable = false
            setCellValueFactory { SimpleStringProperty(it.value.author) }
        }
        val commit = tableColumn<Commit, String> {
            text = "Commit"
            isSortable = false
            setCellValueFactory { SimpleStringProperty(it.value.shortId) }
        }

        val localCommits = TableView<Commit>(logService.commits)
        localCommits.items.addListener(ListChangeListener { localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst() })
        localCommits.columns.addAll(message, date, author, commit)
        localCommits.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        localCommits.selectionModel.selectedItemProperty().addListener { _, _, it -> logService.activeCommit.set(it) }
        localCommits.setOnScroll {
            if (it.deltaY < 0) {
                val index = localCommits.items.size - 1
                logService.logMore()
                localCommits.scrollTo(index)
            }
        }
        localCommits.setOnKeyPressed {
            if (it.code == KeyCode.DOWN && localCommits.selectionModel.selectedItem == localCommits.items.last()) {
                logService.logMore()
                localCommits.scrollTo(localCommits.selectionModel.selectedItem)
            }
        }

        val progressPane = progressPane {
            +splitPane {
                addClass("log-view")
                vgrow(Priority.ALWAYS)
                +localCommits
                +CommitDetailsView()
            }
            +stackPane {
                addClass("overlay")
                visibleWhen(Bindings.isEmpty(localCommits.items))
                +Text("There are no commits.")
            }
        }
        content = progressPane

        logService.logExecutor = progressPane
        logService.logErrorHandler = { errorAlert(window, "Cannot Fetch From Remote", "Please check the repository settings.\nCredentials or proxy settings may have changed.") }

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
                        if (it == branchService.head.get()) addClass("current")
                        text = it.substringAfter("tag:").trim().abbrev()
                        graphic = if (it.startsWith("tag:")) Icons.tag() else Icons.codeFork()
                    }
                }
            }
        }

        private fun String.abbrev() = if (length > 35) "${substring(0, 35)}..." else this

    }

}
