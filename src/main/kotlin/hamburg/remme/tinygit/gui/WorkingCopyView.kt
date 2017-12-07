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
    private val unstageAll = Action("Unstage all", shortcut = "Shortcut+Shift+K", disable = State.canUnstageAll.not(),
            handler = { unstage(State.selectedRepository) })
    private val unstageSelected = Action("Unstage selected", disable = State.canUnstageSelected.not(),
            handler = { unstage(State.selectedRepository, stagedFilesSelection.selectedItems) })
    private val updateAll = Action("Update all", disable = State.canUpdateAll.not(),
            handler = { update(State.selectedRepository) })
    private val stageAll = Action("Stage all", shortcut = "Shortcut+Shift+L", disable = State.canStageAll.not(),
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

        // TODO: should be menu bar actions as well
        val deleteFile = Action("Delete", { FontAwesome.trash() },
                handler = { deleteFile(State.selectedRepository, pendingFilesSelection.selectedItems) })
        val discardChanges = Action("Discard Changes", { FontAwesome.undo() },
                handler = { discardChanges(State.selectedRepository, pendingFilesSelection.selectedItems) })

        pendingFiles.contextMenu = context {
            isAutoHide = true
            +ActionGroup(deleteFile, discardChanges)
        }
        pendingFiles.setOnKeyPressed {
            if (it.code == KeyCode.DELETE) deleteFile(State.selectedRepository, pendingFilesSelection.selectedItems)
        }

        stagedFilesSelection.selectedItems.addListener(ListChangeListener { State.stagedFilesSelected.set(it.list.size) })
        stagedFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { pendingFilesSelection.clearSelection() } })

        pendingFilesSelection.selectedItems.addListener(ListChangeListener { State.pendingFilesSelected.set(it.list.size) })
        pendingFilesSelection.selectedItemProperty().addListener({ _, _, it -> it?.let { stagedFilesSelection.clearSelection() } })

        // TODO: selection is little buggy sometimes / not refreshing correctly
        selectedFile = Bindings.createObjectBinding(
                Callable { stagedFilesSelection.selectedItem ?: pendingFilesSelection.selectedItem },
                stagedFilesSelection.selectedItemProperty(), pendingFilesSelection.selectedItemProperty())
        selectedFile.addListener { _, _, it -> it?.let { fileDiff.update(State.selectedRepository, it) } ?: fileDiff.clear() }

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
            it?.let {
                diff(it)
                State.stashEntries.set(Git.stashListSize(it))
            }
        }
        State.addRefreshListener {
            diff(it)
            State.stashEntries.set(Git.stashListSize(it))
        }
    }

    private fun diff(repository: LocalRepository) {
        task?.cancel()
        task = object : Task<LocalStatus>() {
            override fun call() = Git.status(repository) // TODO: git status might not be good enough here

            override fun succeeded() {
                // TODO: this is still a little buggy sometimes
                val staged = (stagedFiles.selectionModel.selectedItems + stagedFiles.selectionModel.selectedItem).filterNotNull()
                val pending = (pendingFiles.selectionModel.selectedItems + pendingFiles.selectionModel.selectedItem).filterNotNull()

                stagedFiles.items.setAll(value.staged)
                pendingFiles.items.setAll(value.pending)

                if (staged.isNotEmpty()) stagedFiles.selectionModel.selectIndices(-1,
                        *staged.map { stagedFiles.items.indexOf(it) }.toIntArray())
                if (pending.isNotEmpty()) pendingFiles.selectionModel.selectIndices(-1,
                        *pending.map { pendingFiles.items.indexOf(it) }.toIntArray())
            }
        }.also { State.execute(it) }
    }

    private fun stage(repository: LocalRepository) {
        Git.stageAll(repository, pendingFiles.items.filter { it.status == LocalFile.Status.REMOVED && !it.cached })
        diff(repository)
    }

    private fun stage(repository: LocalRepository, files: List<LocalFile>) {
        Git.stage(repository, files)
        diff(repository)
    }

    private fun update(repository: LocalRepository) {
        Git.updateAll(repository)
        diff(repository)
    }

    private fun unstage(repository: LocalRepository) {
        Git.reset(repository)
        diff(repository)
    }

    private fun unstage(repository: LocalRepository, files: List<LocalFile>) {
        Git.reset(repository, files)
        diff(repository)
    }

    private fun deleteFile(repository: LocalRepository, files: List<LocalFile>) {
        if (confirmWarningAlert(window, "Delete Files", "Delete",
                "This will remove the selected files from the disk.")) {
            files.forEach { repository.resolve(it).asPath().delete() }
            diff(repository)
        }
    }

    private fun discardChanges(repository: LocalRepository, files: List<LocalFile>) {
        if (confirmWarningAlert(window, "Discard Changes", "Discard",
                "This will discard all changes from the selected files.")) {
            try {
                Git.checkout(repository, files)
                diff(repository)
            } catch (ex: JGitInternalException) {
                errorAlert(window, "Cannot Discard Changes", "${ex.message}")
            }
        }
    }

}
