package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.TaskExecutor
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.progressIndicator
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.GraphView
import hamburg.remme.tinygit.gui.component.Icons
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.text.Text

class CommitLogView : Tab() {

    private val logService = TinyGit.commitLogService
    private val branchService = TinyGit.branchService
    private val window get() = content.scene.window

    init {
        text = "Commit Log (beta)"
        graphic = Icons.list()
        isClosable = false

        val localCommits = GraphView(logService.commits, branchService.head, branchService.branches)
        localCommits.items.addListener(ListChangeListener { localCommits.selectionModel.selectedItem ?: localCommits.selectionModel.selectFirst() })
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

        val indicator = FetchIndicator()
        content = vbox {
            +toolBar {
                +indicator
                addSpacer()
                +comboBox<CommitLogService.CommitType> {
                    items.addAll(CommitLogService.CommitType.values())
                    valueProperty().bindBidirectional(logService.commitType)
                }
                +comboBox<CommitLogService.Scope> {
                    items.addAll(CommitLogService.Scope.values())
                    valueProperty().bindBidirectional(logService.scope)
                }
            }
            +stackPane {
                vgrow(Priority.ALWAYS)
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
        }

        logService.logExecutor = indicator
        logService.logErrorHandler = { errorAlert(window, "Cannot Fetch From Remote", "Please check the repository settings.\nCredentials or proxy settings may have changed.") }
    }

    private class FetchIndicator : HBoxBuilder(), TaskExecutor {

        private val visible = SimpleBooleanProperty()

        init {
            visibleWhen(visible)
            managedWhen(visibleProperty())
            spacing = 6.0
            alignment = Pos.CENTER
            +progressIndicator(6.0)
            +label { +"Fetching..." }
        }

        override fun execute(task: Task<*>) {
            task.setOnSucceeded { visible.set(false) }
            task.setOnCancelled { visible.set(false) }
            task.setOnFailed { visible.set(false) }
            visible.set(true)
            TinyGit.execute(task)
        }

    }

}
