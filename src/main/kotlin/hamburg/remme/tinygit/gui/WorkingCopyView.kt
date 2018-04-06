package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.service.WorkingCopyService
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.shortName
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.Tab
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import java.util.concurrent.Callable

private const val DEFAULT_STYLE_CLASS = "working-copy-view"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"
private const val FILES_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__files"
private const val OVERLAY_STYLE_CLASS = "overlay"

/**
 * This view is showing the currently state of the working copy and a diff for the selected file.
 * Only one single diff is ever shown and only files from either the staged or the pending section can be
 * selected.
 * Selections will also change the state of [TinyGit.workingCopyService] and [TinyGit.state].
 *
 * The [FileDiffView] will always show the most recently selected file.
 *
 * Both [FileStatusView]s are [SelectionMode.MULTIPLE], with the [WorkingCopyView] automatically deselecting
 * all files if a file on the opposing section has been selected.
 *
 * There are also shortcuts for executing different file actions:
 *  * `L`   - for unstaging files
 *  * `K`   - for staging files
 *  * `Del` - for deleting files from the machine
 *  * `D`   - for discarding all changes from the file
 * These actions can also be triggered with a context menu.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━━━━━━━┓
 *   ┃ ToolBar                  ┃                          ┃
 *   ┠──────────────────────────┨                          ┨
 *   ┃                          ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┃ FileStatusView           ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┣━━━━━━━━━━━━━━━━━━━━━━━━━━┫ FileDiffView             ┃
 *   ┃ ToolBar                  ┃                          ┃
 *   ┠──────────────────────────┨                          ┃
 *   ┃                          ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┃ FileStatusView           ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┃                          ┃                          ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @todo add the actions of the context menu to the menu bar?
 *
 * @see FileStatusView
 * @see FileDiffView
 * @see StatusCountView
 */
class WorkingCopyView : Tab() {

    private val service = TinyGit.get<WorkingCopyService>()
    private val state = TinyGit.get<State>()

    /**
     * Actions to be used in the [GitView]'s menu bar.
     */
    val actions get() = arrayOf(ActionGroup(updateAll, stageAll, stageSelected), ActionGroup(unstageAll, unstageSelected))
    private val unstageAll = Action(I18N["workingCopy.unstageAll"], { Icons.arrowAltCircleDown() }, "Shortcut+Shift+L", state.canUnstageAll.not(),
            { service.unstage() })
    private val unstageSelected = Action(I18N["workingCopy.unstageSelected"], { Icons.arrowAltCircleDown() }, disabled = state.canUnstageSelected.not(),
            handler = { unstageSelected() })
    private val updateAll = Action(I18N["workingCopy.updateAll"], { Icons.arrowAltCircleUp() }, disabled = state.canUpdateAll.not(),
            handler = { service.update() })
    private val stageAll = Action(I18N["workingCopy.stageAll"], { Icons.arrowAltCircleUp() }, "Shortcut+Shift+K", state.canStageAll.not(),
            { service.stage() })
    private val stageSelected = Action(I18N["workingCopy.stageSelected"], { Icons.arrowAltCircleUp() }, disabled = state.canStageSelected.not(),
            handler = { stageSelected() })

    private val staged = FileStatusView(service.staged, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val pending = FileStatusView(service.pending, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val selectedStaged = staged.selectionModel
    private val selectedPending = pending.selectionModel

    init {
        text = I18N["workingCopy.tab"]
        graphic = Icons.hdd()
        isClosable = false

        val unstageKey = KeyCode.L
        val stageKey = KeyCode.K
        val deleteKey = KeyCode.DELETE
        val discardKey = KeyCode.D

        val unstageFile = Action("${I18N["workingCopy.unstage"]} (${unstageKey.shortName})", { Icons.arrowAltCircleDown() }, disabled = state.canUnstageSelected.not(),
                handler = { unstageSelected() })

        staged.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(unstageFile)
        }
        staged.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                unstageKey -> if (state.canUnstageSelected.get()) unstageSelected()
                else -> Unit
            }
        }

        val stageFile = Action("${I18N["workingCopy.stage"]} (${stageKey.shortName})", { Icons.arrowAltCircleUp() }, disabled = state.canStageSelected.not(),
                handler = { stageSelected() })
        val deleteFile = Action("${I18N["workingCopy.delete"]} (${deleteKey.shortName})", { Icons.trash() }, disabled = state.canDeleteSelected.not(),
                handler = { deleteFile() })
        val discardChanges = Action("${I18N["workingCopy.discard"]} (${discardKey.shortName})", { Icons.undo() }, disabled = state.canDiscardSelected.not(),
                handler = { discardChanges() })

        pending.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(stageFile)
            +ActionGroup(deleteFile, discardChanges)
        }
        pending.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                stageKey -> if (state.canStageSelected.get()) stageSelected()
                deleteKey -> if (state.canDeleteSelected.get()) deleteFile()
                discardKey -> if (state.canDiscardSelected.get()) discardChanges()
                else -> Unit
            }
        }

        selectedStaged.selectedItems.addListener(ListChangeListener { service.selectedStaged.setAll(it.list) })
        selectedStaged.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedPending.clearSelection() } })

        selectedPending.selectedItems.addListener(ListChangeListener { service.selectedPending.setAll(it.list) })
        selectedPending.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedStaged.clearSelection() } })

        val fileDiff = FileDiffView(Bindings.createObjectBinding(
                Callable { selectedStaged.selectedItem ?: selectedPending.selectedItem },
                selectedStaged.selectedItemProperty(), selectedPending.selectedItemProperty()))

        content = stackPane {
            addClass(DEFAULT_STYLE_CLASS)

            +splitPane {
                addClass(CONTENT_STYLE_CLASS)

                +splitPane {
                    addClass(FILES_STYLE_CLASS)

                    +vbox {
                        +toolBar {
                            +StatusCountView(staged.items)
                            addSpacer()
                            +unstageAll
                            +unstageSelected
                        }
                        +staged
                    }
                    +vbox {
                        +toolBar {
                            +StatusCountView(pending.items)
                            addSpacer()
                            +updateAll
                            +stageAll
                            +stageSelected
                        }
                        +pending
                    }
                }
                +fileDiff
            }
            +stackPane {
                addClass(OVERLAY_STYLE_CLASS)
                visibleWhen(Bindings.isEmpty(staged.items).and(Bindings.isEmpty(pending.items)))
                managedWhen(visibleProperty())
                +label { text = I18N["workingCopy.nothingToCommit"] }
            }
        }

        TinyGit.addListener { fileDiff.refresh() }
    }

    private fun stageSelected() {
        val selected = getIndex(selectedPending)
        service.stageSelected { setIndex(selectedPending, selected) }
    }

    private fun unstageSelected() {
        val selected = getIndex(selectedStaged)
        service.unstageSelected { setIndex(selectedStaged, selected) }
    }

    private fun deleteFile() {
        if (!confirmWarningAlert(TinyGit.window, I18N["dialog.deleteFiles.header"], I18N["dialog.deleteFiles.button"],
                        I18N["dialog.deleteFiles.text", I18N["workingCopy.selectedFiles", selectedPending.selectedItems.size]])) return
        val selected = getIndex(selectedPending)
        service.delete { setIndex(selectedPending, selected) }
    }

    private fun discardChanges() {
        if (!confirmWarningAlert(TinyGit.window, I18N["dialog.discardChanges.header"], I18N["dialog.discardChanges.button"],
                        I18N["dialog.discardChanges.text", I18N["workingCopy.selectedFiles", selectedPending.selectedItems.size]])) return
        val selected = getIndex(selectedPending)
        service.discardChanges(
                { setIndex(selectedPending, selected) },
                { errorAlert(TinyGit.window, I18N["dialog.cannotDiscard.header"], it) })
    }

    private fun getIndex(selectionModel: MultipleSelectionModel<File>): Int {
        return if (selectionModel.selectedItems.size == 1) selectionModel.selectedIndex else -1
    }

    private fun setIndex(selectionModel: MultipleSelectionModel<File>, index: Int) {
        selectionModel.clearAndSelect(index)
        selectionModel.selectedItem ?: selectionModel.selectLast()
    }

}
