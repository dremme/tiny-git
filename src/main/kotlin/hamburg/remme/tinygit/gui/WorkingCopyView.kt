package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.service.WorkingCopyService
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
import javafx.stage.Window
import java.util.concurrent.Callable

class WorkingCopyView : Tab() {

    val actions: Array<ActionGroup>
        get() = arrayOf(
                ActionGroup(updateAll, stageAll, stageSelected),
                ActionGroup(unstageAll, unstageSelected))
    private val unstageAll = Action("Unstage all", { Icons.arrowCircleDown() }, "Shortcut+Shift+L", State.canUnstageAll.not(),
            { WorkingCopyService.unstage() })
    private val unstageSelected = Action("Unstage selected", { Icons.arrowCircleDown() }, disable = State.canUnstageSelected.not(),
            handler = { unstageSelected() })
    private val updateAll = Action("Update all", { Icons.arrowCircleUp() }, disable = State.canUpdateAll.not(),
            handler = { WorkingCopyService.update() })
    private val stageAll = Action("Stage all", { Icons.arrowCircleUp() }, "Shortcut+Shift+K", State.canStageAll.not(),
            { WorkingCopyService.stage() })
    private val stageSelected = Action("Stage selected", { Icons.arrowCircleUp() }, disable = State.canStageSelected.not(),
            handler = { stageSelected() })

    private val window: Window get() = content.scene.window
    private val staged = FileStatusView(WorkingCopyService.staged, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val pending = FileStatusView(WorkingCopyService.pending, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val selectedStaged = staged.selectionModel
    private val selectedPending = pending.selectionModel

    init {
        text = "Working Copy"
        graphic = Icons.hdd()
        isClosable = false

        val unstageFile = Action("Unstage (L)", { Icons.arrowCircleDown() }, disable = State.canUnstageSelected.not(),
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
        val stageFile = Action("Stage (K)", { Icons.arrowCircleUp() }, disable = State.canStageSelected.not(),
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

        selectedStaged.selectedItems.addListener(ListChangeListener { WorkingCopyService.selectedStaged.setAll(it.list) })
        selectedStaged.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedPending.clearSelection() } })

        selectedPending.selectedItems.addListener(ListChangeListener { WorkingCopyService.selectedPending.setAll(it.list) })
        selectedPending.selectedItemProperty().addListener({ _, _, it -> it?.let { selectedStaged.clearSelection() } })

        content = stackPane {
            +splitPane {
                addClass("working-copy-view")

                +splitPane {
                    addClass("files")

                    +vbox {
                        +toolBar {
                            +StatusCountView(staged)
                            addSpacer()
                            +unstageAll
                            +unstageSelected
                        }
                        +staged
                    }
                    +vbox {
                        +toolBar {
                            +StatusCountView(pending)
                            addSpacer()
                            +updateAll
                            +stageAll
                            +stageSelected
                        }
                        +pending
                    }
                }
                +FileDiffView(Bindings.createObjectBinding(
                        Callable { selectedStaged.selectedItem ?: selectedPending.selectedItem },
                        selectedStaged.selectedItemProperty(), selectedPending.selectedItemProperty()))
            }
            +stackPane {
                addClass("overlay")
                visibleWhen(Bindings.isEmpty(staged.items).and(Bindings.isEmpty(pending.items)))
                +Text("There is nothing to commit.")
            }
        }
    }

    private fun stageSelected() {
        val selected = getIndex(selectedPending)
        WorkingCopyService.stageSelected { setIndex(selectedPending, selected) }
    }

    private fun unstageSelected() {
        val selected = getIndex(selectedStaged)
        WorkingCopyService.unstageSelected { setIndex(selectedStaged, selected) }
    }

    private fun deleteFile() {
        if (confirmWarningAlert(window, "Delete Files", "Delete",
                "This will remove the selected files from the disk.")) {
            val selected = getIndex(selectedPending)
            WorkingCopyService.delete { setIndex(selectedPending, selected) }
        }
    }

    private fun discardChanges() {
        if (confirmWarningAlert(window, "Discard Changes", "Discard",
                "This will discard all changes from the selected files.")) {
            val selected = getIndex(selectedPending)
            WorkingCopyService.discardChanges(
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
