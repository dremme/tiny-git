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

    private val service = TinyGit.commitLogService
    private val window get() = content.scene.window

    init {
        text = "Commit Log (beta)"
        graphic = Icons.list()
        isClosable = false

        val graph = GraphView(service.commits, service.commitGraph)
        graph.items.addListener(ListChangeListener { graph.selectionModel.selectedItem ?: graph.selectionModel.selectFirst() })
        graph.selectionModel.selectedItemProperty().addListener { _, _, it -> service.activeCommit.set(it) }
        graph.setOnScroll {
            // TODO: buggy
            if (it.deltaY < 0) {
                val index = graph.items.size - 1
                service.logMore()
                graph.scrollTo(index)
            }
        }
        graph.setOnKeyPressed {
            if (it.code == KeyCode.DOWN && graph.selectionModel.selectedItem == graph.items.last()) {
                service.logMore()
                graph.scrollTo(graph.selectionModel.selectedItem)
            }
        }

        val indicator = FetchIndicator()
        content = vbox {
            +toolBar {
                +indicator
                addSpacer()
                +comboBox<CommitLogService.CommitType> {
                    items.addAll(CommitLogService.CommitType.values())
                    valueProperty().bindBidirectional(service.commitType)
                    valueProperty().addListener { _, _, it -> graph.graphVisible.set(!it.isNoMerges) }
                }
                +comboBox<CommitLogService.Scope> {
                    items.addAll(CommitLogService.Scope.values())
                    valueProperty().bindBidirectional(service.scope)
                }
            }
            +stackPane {
                vgrow(Priority.ALWAYS)
                +splitPane {
                    addClass("log-view")
                    vgrow(Priority.ALWAYS)
                    +graph
                    +CommitDetailsView()
                }
                +stackPane {
                    addClass("overlay")
                    visibleWhen(Bindings.isEmpty(graph.items))
                    +Text("There are no commits.")
                }
            }
        }

        service.logExecutor = indicator
        service.logErrorHandler = { errorAlert(window, "Cannot Fetch From Remote", "Please check the repository settings.\nCredentials or proxy settings may have changed.") }
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
