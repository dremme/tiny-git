package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.progressPane
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.GraphView
import hamburg.remme.tinygit.gui.component.Icons
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
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
    }

}
