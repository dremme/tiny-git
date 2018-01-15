package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.delete
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Status
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.gitAdd
import hamburg.remme.tinygit.git.gitAddUpdate
import hamburg.remme.tinygit.git.gitCheckout
import hamburg.remme.tinygit.git.gitRemove
import hamburg.remme.tinygit.git.gitReset
import hamburg.remme.tinygit.git.gitStatus
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
import javafx.beans.value.ObservableObjectValue
import javafx.collections.FXCollections
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
            { unstage(State.getSelectedRepository()) })
    private val unstageSelected = Action("Unstage selected", { Icons.arrowCircleDown() }, disable = State.canUnstageSelected.not(),
            handler = { unstage(State.getSelectedRepository(), stagedFilesSelection.selectedItems) })
    private val updateAll = Action("Update all", { Icons.arrowCircleUp() }, disable = State.canUpdateAll.not(),
            handler = { update(State.getSelectedRepository()) })
    private val stageAll = Action("Stage all", { Icons.arrowCircleUp() }, "Shortcut+Shift+K", State.canStageAll.not(),
            { stage(State.getSelectedRepository()) })
    private val stageSelected = Action("Stage selected", { Icons.arrowCircleUp() }, disable = State.canStageSelected.not(),
            handler = {
                if (pendingFiles.items.size == pendingFilesSelection.selectedItems.size) stage(State.getSelectedRepository())
                else stage(State.getSelectedRepository(), pendingFilesSelection.selectedItems)
            })

    private val window: Window get() = content.scene.window
    private val stagedFiles = FileStatusView(State.stagedFiles, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val pendingFiles = FileStatusView(State.pendingFiles, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val stagedFilesSelection = stagedFiles.selectionModel
    private val pendingFilesSelection = pendingFiles.selectionModel
    private val selectedFile: ObservableObjectValue<File>
    private val fileDiff = FileDiffView()
//    private var task: Task<*>? = null

    init {
        text = "Working Copy"
        graphic = Icons.hdd()
        isClosable = false

        val unstageFile = Action("Unstage (L)", { Icons.arrowCircleDown() }, disable = State.canUnstageSelected.not(),
                handler = { unstage(State.getSelectedRepository(), stagedFilesSelection.selectedItems) })

        stagedFiles.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(unstageFile)
        }
        stagedFiles.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                KeyCode.L -> unstage(State.getSelectedRepository(), stagedFilesSelection.selectedItems)
                else -> Unit
            }
        }

        // TODO: state props?
        val canDelete = Bindings.createBooleanBinding(
                Callable { !pendingFilesSelection.selectedItems.all { it.status == File.Status.REMOVED } },
                pendingFilesSelection.selectedIndexProperty())
        val canDiscard = Bindings.createBooleanBinding(
                Callable { !pendingFilesSelection.selectedItems.all { it.status == File.Status.ADDED } },
                pendingFilesSelection.selectedIndexProperty())
        // TODO: menubar actions?
        val stageFile = Action("Stage (K)", { Icons.arrowCircleUp() }, disable = State.canStageSelected.not(),
                handler = { stage(State.getSelectedRepository(), pendingFilesSelection.selectedItems) })
        val deleteFile = Action("Delete (Del)", { Icons.trash() }, disable = canDelete.not(),
                handler = { deleteFile(State.getSelectedRepository(), pendingFilesSelection.selectedItems) })
        val discardChanges = Action("Discard Changes (D)", { Icons.undo() }, disable = canDiscard.not(),
                handler = { discardChanges(State.getSelectedRepository(), pendingFilesSelection.selectedItems) })

        pendingFiles.contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(stageFile)
            +ActionGroup(deleteFile, discardChanges)
        }
        pendingFiles.setOnKeyPressed {
            if (!it.isShortcutDown) when (it.code) {
                KeyCode.K -> stage(State.getSelectedRepository(), pendingFilesSelection.selectedItems)
                KeyCode.D -> discardChanges(State.getSelectedRepository(), pendingFilesSelection.selectedItems)
                KeyCode.DELETE -> deleteFile(State.getSelectedRepository(), pendingFilesSelection.selectedItems)
                else -> Unit
            }
        }

        stagedFilesSelection.selectedItems.addListener(ListChangeListener { State.stagedSelectedCount.set(it.list.size) })
        stagedFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { pendingFilesSelection.clearSelection() } })

        pendingFilesSelection.selectedItems.addListener(ListChangeListener { State.pendingSelectedCount.set(it.list.size) })
        pendingFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { stagedFilesSelection.clearSelection() } })

        selectedFile = Bindings.createObjectBinding(
                Callable { stagedFilesSelection.selectedItem ?: pendingFilesSelection.selectedItem },
                stagedFilesSelection.selectedItemProperty(), pendingFilesSelection.selectedItemProperty())
        selectedFile.addListener { _, _, it -> it?.let { fileDiff.update(State.getSelectedRepository(), it) } ?: fileDiff.clearContent() }

        content = stackPane {
            +splitPane {
                addClass("working-copy-view")

                +splitPane {
                    addClass("files")

                    +vbox {
                        +toolBar {
                            +StatusCountView(stagedFiles)
                            addSpacer()
                            +unstageAll
                            +unstageSelected
                        }
                        +stagedFiles
                    }
                    +vbox {
                        +toolBar {
                            +StatusCountView(pendingFiles)
                            addSpacer()
                            +updateAll
                            +stageAll
                            +stageSelected
                        }
                        +pendingFiles
                    }
                }
                +fileDiff
            }
            +stackPane {
                addClass("overlay")
                visibleWhen(Bindings.isEmpty(stagedFiles.items).and(Bindings.isEmpty(pendingFiles.items)))
                +Text("There is nothing to commit.")
            }
        }

        State.addRepositoryListener {
            clearContent()
            it?.let { status(it) }
        }
        State.addRefreshListener(this) { status(it) }
    }

    // TODO: sort by status?
    private fun setContent(status: Status) {
        stagedFiles.items.addAll(status.staged.filter { stagedFiles.items.none(it::equals) })
        stagedFiles.items.removeAll(stagedFiles.items.filter { status.staged.none(it::equals) })
        pendingFiles.items.addAll(status.pending.filter { pendingFiles.items.none(it::equals) })
        pendingFiles.items.removeAll(pendingFiles.items.filter { status.pending.none(it::equals) })
        FXCollections.sort(stagedFiles.items)
        FXCollections.sort(pendingFiles.items)
    }

    private fun clearContent() {
//        task?.cancel()
        stagedFiles.items.clear()
        pendingFiles.items.clear()
    }

    private fun status(repository: Repository, block: (() -> Unit)? = null) {
        // TODO: task needed?
//        task?.cancel()
//        task = object : Task<GitStatus>() {
//            override fun call() = gitStatus(repository)
//
//            override fun succeeded() {
//                setContent(value)
//                block?.invoke()
//            }
//        }.also { State.execute(it) }
        setContent(gitStatus(repository))
        block?.invoke()
    }

    private fun stage(repository: Repository) {
        gitAdd(repository)
        gitRemove(repository, pendingFiles.items.filter { it.status == File.Status.REMOVED })
        status(repository)
    }

    private fun stage(repository: Repository, files: List<File>) {
        val selected = getIndex(pendingFilesSelection)
        gitAdd(repository, files.filter { it.status != File.Status.REMOVED })
        gitRemove(repository, files.filter { it.status == File.Status.REMOVED })
        status(repository) { setIndex(pendingFilesSelection, selected) }
    }

    private fun update(repository: Repository) {
        gitAddUpdate(repository)
        status(repository)
    }

    private fun unstage(repository: Repository) {
        gitReset(repository)
        status(repository)
    }

    private fun unstage(repository: Repository, files: List<File>) {
        val selected = getIndex(stagedFilesSelection)
        gitReset(repository, files)
        status(repository) { setIndex(stagedFilesSelection, selected) }
    }

    private fun deleteFile(repository: Repository, files: List<File>) {
        if (confirmWarningAlert(window, "Delete Files", "Delete",
                "This will remove the selected files from the disk.")) {
            val selected = getIndex(pendingFilesSelection)
            files.forEach { repository.resolve(it).asPath().takeIf { it.exists() }?.delete() }
            status(repository) { setIndex(pendingFilesSelection, selected) }
        }
    }

    private fun discardChanges(repository: Repository, files: List<File>) {
        if (confirmWarningAlert(window, "Discard Changes", "Discard",
                "This will discard all changes from the selected files.")) {
            try {
                gitCheckout(repository, files)
                status(repository)
            } catch (ex: RuntimeException) { // TODO
                errorAlert(window, "Cannot Discard Changes", "${ex.message}")
            }
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
