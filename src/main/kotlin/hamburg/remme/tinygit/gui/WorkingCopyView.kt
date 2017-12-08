package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.delete
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStatus
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.context
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableObjectValue
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.scene.control.SelectionMode
import javafx.scene.control.Tab
import javafx.scene.input.KeyCode
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Window
import org.eclipse.jgit.api.errors.JGitInternalException
import java.util.concurrent.Callable

class WorkingCopyView : Tab() {

    val actions: Array<ActionGroup>
        get() = arrayOf(ActionGroup(updateAll, stageAll, stageSelected), ActionGroup(unstageAll, unstageSelected))
    private val unstageAll = Action("Unstage all", shortcut = "Shortcut+Shift+L", disable = State.canUnstageAll.not(),
            handler = { unstage(State.selectedRepository) })
    private val unstageSelected = Action("Unstage selected", disable = State.canUnstageSelected.not(),
            handler = { unstage(State.selectedRepository, stagedFilesSelection.selectedItems) })
    private val updateAll = Action("Update all", disable = State.canUpdateAll.not(),
            handler = { update(State.selectedRepository) })
    private val stageAll = Action("Stage all", shortcut = "Shortcut+Shift+K", disable = State.canStageAll.not(),
            handler = { stage(State.selectedRepository) })
    private val stageSelected = Action("Stage selected", disable = State.canStageSelected.not(),
            handler = {
                if (isAllSelected) stage(State.selectedRepository)
                else stage(State.selectedRepository, pendingFilesSelection.selectedItems)
            })

    private val window: Window get() = content.scene.window
    private val stagedFiles = FileStatusView(State.stagedFiles, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val pendingFiles = FileStatusView(State.pendingFiles, SelectionMode.MULTIPLE).vgrow(Priority.ALWAYS)
    private val stagedFilesSelection = stagedFiles.selectionModel
    private val pendingFilesSelection = pendingFiles.selectionModel
    private val selectedFile: ObservableObjectValue<LocalFile>
    private val fileDiff = FileDiffView()
    private val isAllSelected: Boolean get() = pendingFiles.items.size == pendingFilesSelection.selectedItems.size
    private var task: Task<*>? = null

    init {
        text = "Working Copy"
        graphic = FontAwesome.desktop()
        isClosable = false

        val unstageFile = Action("Unstage", disable = State.canUnstageSelected.not(),
                handler = { unstage(State.selectedRepository, stagedFilesSelection.selectedItems) })

        stagedFiles.contextMenu = context {
            isAutoHide = true
            +ActionGroup(unstageFile)
        }
        stagedFiles.setOnKeyPressed {
            if (it.code == KeyCode.L) unstage(State.selectedRepository, stagedFilesSelection.selectedItems)
        }

        // TODO: menubar actions?
        val canDelete = State.canStageSelected // TODO: own binding?
        val canDiscard = Bindings.createBooleanBinding(
                Callable { !pendingFilesSelection.selectedItems.all { it.status == LocalFile.Status.ADDED } },
                pendingFilesSelection.selectedItemProperty())
        val stageFile = Action("Stage", disable = State.canStageSelected.not(),
                handler = { stage(State.selectedRepository, pendingFilesSelection.selectedItems) })
        val deleteFile = Action("Delete", { FontAwesome.trash() }, disable = canDelete.not(),
                handler = { deleteFile(State.selectedRepository, pendingFilesSelection.selectedItems) })
        val discardChanges = Action("Discard Changes", { FontAwesome.undo() }, disable = canDiscard.not(),
                handler = { discardChanges(State.selectedRepository, pendingFilesSelection.selectedItems) })

        pendingFiles.contextMenu = context {
            isAutoHide = true
            +ActionGroup(stageFile)
            +ActionGroup(deleteFile, discardChanges)
        }
        pendingFiles.setOnKeyPressed {
            when (it.code) {
                KeyCode.K -> stage(State.selectedRepository, pendingFilesSelection.selectedItems)
                KeyCode.DELETE -> deleteFile(State.selectedRepository, pendingFilesSelection.selectedItems)
                else -> Unit
            }
        }

        stagedFilesSelection.selectedItems.addListener(ListChangeListener { State.stagedFilesSelected.set(it.list.size) })
        stagedFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { pendingFilesSelection.clearSelection() } })

        pendingFilesSelection.selectedItems.addListener(ListChangeListener { State.pendingFilesSelected.set(it.list.size) })
        pendingFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { stagedFilesSelection.clearSelection() } })

        // TODO: selection is little buggy sometimes / not refreshing correctly
        selectedFile = Bindings.createObjectBinding(
                Callable { stagedFilesSelection.selectedItem ?: pendingFilesSelection.selectedItem },
                stagedFilesSelection.selectedItemProperty(), pendingFilesSelection.selectedItemProperty())
        selectedFile.addListener { _, _, it -> it?.let { fileDiff.update(State.selectedRepository, it) } ?: fileDiff.clearContent() }

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
            status(it)
            State.stashEntries.set(Git.stashListSize(it))
        }
        State.addRefreshListener {
            status(it)
            State.stashEntries.set(Git.stashListSize(it))
        }
    }

    private fun updateStatus(status: LocalStatus) {
        stagedFiles.items.setAll(status.staged)
        pendingFiles.items.setAll(status.pending)
    }

    private fun status(repository: LocalRepository, block: (() -> Unit)? = null) {
        task?.cancel()
        task = object : Task<LocalStatus>() {
            override fun call() = Git.status(repository)

            override fun succeeded() {
                if (block != null) {
                    updateStatus(value)
                    block.invoke()
                } else {
                    val staged = (stagedFilesSelection.selectedItems + stagedFilesSelection.selectedItem).filterNotNull()
                    val pending = (pendingFilesSelection.selectedItems + pendingFilesSelection.selectedItem).filterNotNull()
                    updateStatus(value)
                    staged.map { stagedFiles.items.indexOf(it) }.forEach { stagedFilesSelection.select(it) }
                    pending.map { pendingFiles.items.indexOf(it) }.forEach { pendingFilesSelection.select(it) }
                }
            }
        }.also { State.execute(it) }
    }

    private fun stage(repository: LocalRepository) {
        Git.stageAll(repository, pendingFiles.items.filter { it.status == LocalFile.Status.REMOVED && !it.cached })
        status(repository)
    }

    private fun stage(repository: LocalRepository, files: List<LocalFile>) {
        val selectedPending = if (files.size == 1) pendingFilesSelection.selectedIndex else -1
        Git.stage(repository, files)
        status(repository) {
            pendingFilesSelection.select(selectedPending)
            pendingFilesSelection.selectedItem ?: pendingFilesSelection.selectLast()
        }
    }

    private fun update(repository: LocalRepository) {
        Git.updateAll(repository)
        status(repository)
    }

    private fun unstage(repository: LocalRepository) {
        Git.reset(repository)
        status(repository)
    }

    private fun unstage(repository: LocalRepository, files: List<LocalFile>) {
        val selectedStaged = if (files.size == 1) stagedFilesSelection.selectedIndex else -1
        Git.reset(repository, files)
        status(repository) {
            stagedFilesSelection.select(selectedStaged)
            stagedFilesSelection.selectedItem ?: stagedFilesSelection.selectLast()
        }
    }

    private fun deleteFile(repository: LocalRepository, files: List<LocalFile>) {
        if (confirmWarningAlert(window, "Delete Files", "Delete",
                "This will remove the selected files from the disk.")) {
            files.forEach { repository.resolve(it).asPath().delete() }
            status(repository)
        }
    }

    private fun discardChanges(repository: LocalRepository, files: List<LocalFile>) {
        if (confirmWarningAlert(window, "Discard Changes", "Discard",
                "This will discard all changes from the selected files.")) {
            try {
                Git.checkout(repository, files)
                status(repository)
            } catch (ex: JGitInternalException) {
                errorAlert(window, "Cannot Discard Changes", "${ex.message}")
            }
        }
    }

}
