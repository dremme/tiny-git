package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.service.CommitLogService
import hamburg.remme.tinygit.domain.service.TaskListener
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.HBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
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
import hamburg.remme.tinygit.gui.component.GraphListView
import hamburg.remme.tinygit.gui.component.Icons
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.Tab
import javafx.scene.layout.Priority
import javafx.scene.text.Text

private const val DEFAULT_STYLE_CLASS = "commit-log-view"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"

/**
 * Displaying basically the output of `git log`. Each log entry can be selected to display the details of that
 * [Commit].
 * This is relying heavily on the [GraphListView] and its skin for displaying the log graph and commit list.
 *
 * There is also a context menu added to the [GraphListView] for commit related actions.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 *   ┃ ToolBar                              ┃
 *   ┠──────────────────────────────────────┨
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃ GraphListView                        ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┃                                      ┃
 *   ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
 *   ┃                                      ┃
 *   ┃ CommitDetailsView                    ┃
 *   ┃                                      ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @todo should the actions be exposed and used in the menu bar as well?!
 * @todo loading more commits while scrolling down is buggy
 *
 * @see GraphListView
 * @see CommitDetailsView
 */
class CommitLogView : Tab() {

    private val state = TinyGit.state
    private val logService = TinyGit.commitLogService
    private val branchService = TinyGit.branchService
    private val window get() = content.scene.window
    private val graph = GraphListView(logService.commits)
    private val graphSelection get() = graph.selectionModel.selectedItem

    init {
        text = I18N["commitLog.tab"]
        graphic = Icons.list()
        isClosable = false

        val checkoutCommit = Action(I18N["commitLog.checkout"], { Icons.check() }, disabled = state.canCheckoutCommit.not(),
                handler = { checkoutCommit(graphSelection) })
        val resetToCommit = Action(I18N["commitLog.reset"], { Icons.refresh() }, disabled = state.canResetToCommit.not(),
                handler = { resetToCommit(graphSelection) })
        // TODO
        val tagCommit = Action(I18N["commitLog.tag"], { Icons.tag() }, disabled = state.canTagCommit.not(),
                handler = { tagCommit(graphSelection) })

        graph.items.addListener(ListChangeListener { graph.selectionModel.selectedItem ?: graph.selectionModel.selectFirst() })
        graph.selectionModel.selectedItemProperty().addListener { _, _, it -> logService.activeCommit.set(it) }
        graph.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(checkoutCommit, resetToCommit)
        }
//        TODO
//        graph.setOnScroll {
//            if (it.deltaY < 0) {
//                val index = graph.items.size - 1
//                service.logMore()
//                graph.scrollTo(index)
//            }
//        }
//        graph.setOnKeyPressed {
//            if (it.code == KeyCode.DOWN && graph.selectionModel.selectedItem == graph.items.last()) {
//                service.logMore()
//                graph.scrollTo(graph.selectionModel.selectedItem)
//            }
//        }

        val indicator = FetchIndicator()
        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)

            +toolBar {
                +indicator
                addSpacer()
                +comboBox<CommitLogService.CommitType> {
                    items.addAll(CommitLogService.CommitType.values())
                    valueProperty().bindBidirectional(logService.commitType)
                    valueProperty().addListener { _, _, it -> graph.isGraphVisible = !it.isNoMerges }
                }
                +comboBox<CommitLogService.Scope> {
                    items.addAll(CommitLogService.Scope.values())
                    valueProperty().bindBidirectional(logService.scope)
                }
            }
            +stackPane {
                vgrow(Priority.ALWAYS)
                +splitPane {
                    addClass(CONTENT_STYLE_CLASS)
                    vgrow(Priority.ALWAYS)
                    +graph
                    +CommitDetailsView()
                }
                +stackPane {
                    addClass("overlay")
                    visibleWhen(Bindings.isEmpty(graph.items))
                    +Text(I18N["commitLog.noCommits"])
                }
            }
        }

        logService.logListener = indicator
        logService.logErrorHandler = { errorAlert(window, I18N["dialog.cannotFetch.header"], it) }
    }

    private fun checkoutCommit(commit: Commit) {
        branchService.checkoutCommit(
                commit,
                { errorAlert(window, I18N["dialog.cannotCheckout.header"], I18N["dialog.cannotCheckout.text"]) })
    }

    private fun resetToCommit(commit: Commit) {
        if (!confirmWarningAlert(window, I18N["dialog.resetBranch.header"], I18N["dialog.resetBranch.button"], I18N["dialog.resetBranch.text", commit.shortId])) return
        branchService.reset(commit)
    }

    private fun tagCommit(commit: Commit) {
        // TODO
    }

    /**
     * An indicator to be shown in the toolbar while fetching from remote.
     */
    private class FetchIndicator : HBoxBuilder(), TaskListener {

        private val visible = SimpleBooleanProperty()

        init {
            visibleWhen(visible)
            managedWhen(visibleProperty())
            +progressIndicator(6.0)
            +label { +I18N["commitLog.fetching"] }
        }

        override fun started() = visible.set(true)

        override fun done() = visible.set(false)

    }

}
