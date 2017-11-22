package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStatus
import javafx.beans.binding.Bindings
import javafx.beans.value.ObservableObjectValue
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.SelectionMode
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class WorkingCopyView : Tab() {

    val actions: Array<ActionGroup>
        get() = arrayOf(ActionGroup(updateAll, stageAll, stageSelected), ActionGroup(unstageAll, unstageSelected))
    private val unstageAll = Action("Unstage all", shortcut = "Shortcut+Shift+Minus", disable = State.canUnstageAll.not(),
            action = EventHandler { unstage(State.selectedRepository) })
    private val unstageSelected = Action("Unstage selected", disable = State.canUnstageSelected.not(),
            action = EventHandler { unstage(State.selectedRepository, stagedFilesSelection.selectedItems) })
    private val updateAll = Action("Update all", disable = State.canStageAll.not(),
            action = EventHandler { update(State.selectedRepository) })
    private val stageAll = Action("Stage all", shortcut = "Shortcut+Shift+Plus", disable = State.canStageAll.not(),
            action = EventHandler { stage(State.selectedRepository) })
    private val stageSelected = Action("Stage selected", disable = State.canStageSelected.not(),
            action = EventHandler { stage(State.selectedRepository, pendingFilesSelection.selectedItems) })

    private val stagedFiles = FileStatusView(SelectionMode.MULTIPLE)
    private val pendingFiles = FileStatusView(SelectionMode.MULTIPLE)
    private val stagedFilesSelection = stagedFiles.selectionModel
    private val pendingFilesSelection = pendingFiles.selectionModel
    private val selectedFile: ObservableObjectValue<LocalFile>
    private val fileDiff = FileDiffView()
    private var task: Task<*>? = null

    init {
        text = "Working Copy"
        graphic = FontAwesome.desktop()
        isClosable = false

        stagedFiles.items.addListener(ListChangeListener { State.stagedFiles.set(it.list.size) })
        stagedFilesSelection.selectedItems.addListener(ListChangeListener { State.stagedFilesSelected.set(it.list.size) })
        stagedFilesSelection.selectedItemProperty().addListener({ _, _, it ->
            it?.let { pendingFilesSelection.clearSelection() }
        })

        pendingFiles.items.addListener(ListChangeListener { State.pendingFiles.set(it.list.size) })
        pendingFilesSelection.selectedItems.addListener(ListChangeListener { State.pendingFilesSelected.set(it.list.size) })
        pendingFilesSelection.selectedItemProperty().addListener({ _, _, it ->
            it?.let { stagedFilesSelection.clearSelection() }
        })

        selectedFile = Bindings.createObjectBinding(
                { stagedFilesSelection.selectedItem ?: pendingFilesSelection.selectedItem },
                arrayOf(stagedFilesSelection.selectedItemProperty(),
                        pendingFilesSelection.selectedItemProperty()))
        selectedFile.addListener { _, _, it ->
            it?.let { fileDiff.update(State.selectedRepository, it) } ?: fileDiff.clear()
        }

        VBox.setVgrow(stagedFiles, Priority.ALWAYS)
        VBox.setVgrow(pendingFiles, Priority.ALWAYS)

        val files = SplitPane(
                VBox(
                        ToolBar(StatusCountView(stagedFiles),
                                spacer(),
                                button(unstageAll),
                                button(unstageSelected)),
                        stagedFiles),
                VBox(
                        ToolBar(StatusCountView(pendingFiles),
                                spacer(),
                                button(updateAll),
                                button(stageAll),
                                button(stageSelected)),
                        pendingFiles))
        files.styleClass += "files"

        content = SplitPane(files, fileDiff).addClass("working-copy-view")

        State.addRepositoryListener {
            it?.let {
                diff(it)
                State.stashEntries.set(LocalGit.stashListSize(it))
            }
        }
        State.addRefreshListener {
            diff(it)
            State.stashEntries.set(LocalGit.stashListSize(it))
        }
    }

    private fun diff(repository: LocalRepository) {
        println("Status for working copy: $repository")
        task?.cancel()
        task = object : Task<LocalStatus>() {
            override fun call() = LocalGit.status(repository) // TODO: git status might not be good enough here

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
        LocalGit.addAll(repository)
        diff(repository)
    }

    private fun stage(repository: LocalRepository, files: List<LocalFile>) {
        LocalGit.add(repository, files)
        diff(repository)
    }

    private fun update(repository: LocalRepository) {
        LocalGit.addAllUpdate(repository)
        diff(repository)
    }

    private fun unstage(repository: LocalRepository) {
        LocalGit.reset(repository)
        diff(repository)
    }

    private fun unstage(repository: LocalRepository, files: List<LocalFile>) {
        LocalGit.reset(repository, files)
        diff(repository)
    }

}
