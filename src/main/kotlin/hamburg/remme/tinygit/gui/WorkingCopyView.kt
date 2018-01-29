package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.Icons
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.Tab
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import java.util.concurrent.Callable

class WorkingCopyView : Tab() {

    private val workingService = TinyGit.workingCopyService
    private val state = TinyGit.state
    private val window get() = content.scene.window

    val actions get() = arrayOf(ActionGroup(updateAll, stageAll, stageSelected), ActionGroup(unstageAll, unstageSelected))
    private val unstageAll = Action("Unstage all", { Icons.arrowCircleDown() }, "Shortcut+Shift+L", state.canUnstageAll.not(),
            { workingService.unstage() })
    private val unstageSelected = Action("Unstage selected", { Icons.arrowCircleDown() }, disable = state.canUnstageSelected.not(),
            handler = { unstageSelected() })
    private val updateAll = Action("Update all", { Icons.arrowCircleUp() }, disable = state.canUpdateAll.not(),
            handler = { workingService.update() })
    private val stageAll = Action("Stage all", { Icons.arrowCircleUp() }, "Shortcut+Shift+K", state.canStageAll.not(),
            { workingService.stage() })
    private val stageSelected = Action("Stage selected", { Icons.arrowCircleUp() }, disable = state.canStageSelected.not(),
            handler = { stageSelected() })

    private val staged = FileStatusView(workingService.staged, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val pending = FileStatusView(workingService.pending, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val selectedStaged = staged.selectionModel
    private val selectedPending = pending.selectionModel

    init {
        text = "Working Copy"
        graphic = Icons.hdd()
        isClosable = false

        val unstageFile = Action("Unstage (L)", { Icons.arrowCircleDown() }, disable = state.canUnstageSelected.not(),
                handler = { unstageSelected() })

        staged.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(unstageFile)
        }
        staged.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                KeyCode.L -> unstageSelected()
                else -> Unit
            }
        }

        // TODO: state props?
        val canDelete = Bindings.createBooleanBinding(
                Callable { !selectedPending.selectedItems.all { it.status == File.Status.REMOVED } },
                selectedPending.selectedIndexProperty())
        val canDiscard = Bindings.createBooleanBinding(
                Callable { !selectedPending.selectedItems.all { it.status == File.Status.ADDED } },
                selectedPending.selectedIndexProperty())
        // TODO: menubar actions?
        val stageFile = Action("Stage (K)", { Icons.arrowCircleUp() }, disable = state.canStageSelected.not(),
                handler = { stageSelected() })
        val deleteFile = Action("Delete (Del)", { Icons.trash() }, disable = canDelete.not(),
                handler = { deleteFile() })
        val discardChanges = Action("Discard Changes (D)", { Icons.undo() }, disable = canDiscard.not(),
                handler = { discardChanges() })

        pending.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(stageFile)
            +ActionGroup(deleteFile, discardChanges)
        }
        pending.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                KeyCode.K -> stageSelected()
                KeyCode.D -> discardChanges()
                KeyCode.DELETE -> deleteFile()
                else -> Unit
            }
        }

        selectedStaged.selectedItems.addListener(ListChangeListener { workingService.selectedStaged.setAll(it.list) })
        selectedStaged.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedPending.clearSelection() } })

        selectedPending.selectedItems.addListener(ListChangeListener { workingService.selectedPending.setAll(it.list) })
        selectedPending.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedStaged.clearSelection() } })

        val fileDiff = FileDiffView(Bindings.createObjectBinding(
                Callable { selectedStaged.selectedItem ?: selectedPending.selectedItem },
                selectedStaged.selectedItemProperty(), selectedPending.selectedItemProperty()))

        content = stackPane {
            +splitPane {
                addClass("working-copy-view")

                +splitPane {
                    addClass("files")

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
                addClass("overlay")
                visibleWhen(Bindings.isEmpty(staged.items).and(Bindings.isEmpty(pending.items)))
                +Text("There is nothing to commit.")
            }
        }

        TinyGit.addListener { fileDiff.refresh() }
    }

    private fun stageSelected() {
        val selected = getIndex(selectedPending)
        workingService.stageSelected { setIndex(selectedPending, selected) }
    }

    private fun unstageSelected() {
        val selected = getIndex(selectedStaged)
        workingService.unstageSelected { setIndex(selectedStaged, selected) }
    }

    private fun deleteFile() {
        if (confirmWarningAlert(window, "Delete Files", "Delete",
                "This will remove the selected files from the disk.")) {
            val selected = getIndex(selectedPending)
            workingService.delete { setIndex(selectedPending, selected) }
        }
    }

    private fun discardChanges() {
        if (confirmWarningAlert(window, "Discard Changes", "Discard",
                "This will discard all changes from the selected files.")) {
            val selected = getIndex(selectedPending)
            workingService.discardChanges(
                    { setIndex(selectedPending, selected) },
                    { errorAlert(window, "Cannot Discard Changes", it) })
        }
    }

    private fun getIndex(selectionModel: MultipleSelectionModel<File>): Int {
        return if (selectionModel.selectedItems.size == 1) selectionModel.selectedIndex else -1
    }

    private fun setIndex(selectionModel: MultipleSelectionModel<File>, index: Int) {
        selectionModel.clearAndSelect(index)
        selectionModel.selectedItem ?: selectionModel.selectLast()
    }

}
