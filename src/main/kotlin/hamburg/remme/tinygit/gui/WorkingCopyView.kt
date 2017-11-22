package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStatus
import hamburg.remme.tinygit.gui.FontAwesome.desktop
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

    private val stagedFiles = FileStatusView(SelectionMode.MULTIPLE)
    private val unstagedFiles = FileStatusView(SelectionMode.MULTIPLE)
    private val selectedFile: ObservableObjectValue<LocalFile>
    private val fileDiff = FileDiffView()
    private var task: Task<*>? = null

    init {
        text = "Working Copy"
        graphic = desktop()
        isClosable = false

        val unstageAll = button("Unstage all",
                action = EventHandler { unstage(State.getSelectedRepository()) })
        val unstageSelected = button("Unstage selected",
                action = EventHandler { unstage(State.getSelectedRepository(), stagedFiles.selectionModel.selectedItems) })
        unstageAll.disableProperty().bind(Bindings.isEmpty(stagedFiles.items))
        unstageSelected.disableProperty().bind(Bindings.isEmpty(stagedFiles.selectionModel.selectedItems))
        stagedFiles.items.addListener(ListChangeListener { State.stagedFiles.set(it.list.size) })
        stagedFiles.selectionModel.selectedItemProperty().addListener({ _, _, it ->
            it?.let { unstagedFiles.selectionModel.clearSelection() }
        })

        val update = button("Update all",
                action = EventHandler { update(State.getSelectedRepository()) })
        val stageAll = button("Stage all",
                action = EventHandler { stage(State.getSelectedRepository()) })
        val stageSelected = button("Stage selected",
                action = EventHandler { stage(State.getSelectedRepository(), unstagedFiles.selectionModel.selectedItems) })
        update.disableProperty().bind(Bindings.isEmpty(unstagedFiles.items))
        stageAll.disableProperty().bind(Bindings.isEmpty(unstagedFiles.items))
        stageSelected.disableProperty().bind(Bindings.isEmpty(unstagedFiles.selectionModel.selectedItems))
        unstagedFiles.items.addListener(ListChangeListener { State.unstagedFiles.set(it.list.size) })
        unstagedFiles.selectionModel.selectedItemProperty().addListener({ _, _, it ->
            it?.let { stagedFiles.selectionModel.clearSelection() }
        })

        selectedFile = Bindings.createObjectBinding(
                { stagedFiles.selectionModel.selectedItem ?: unstagedFiles.selectionModel.selectedItem },
                arrayOf(stagedFiles.selectionModel.selectedItemProperty(),
                        unstagedFiles.selectionModel.selectedItemProperty()))
        selectedFile.addListener { _, _, it ->
            it?.let { fileDiff.update(State.getSelectedRepository(), it) } ?: fileDiff.clear()
        }

        VBox.setVgrow(stagedFiles, Priority.ALWAYS)
        VBox.setVgrow(unstagedFiles, Priority.ALWAYS)

        val files = SplitPane(
                VBox(
                        ToolBar(StatusCountView(stagedFiles),
                                spacer(),
                                unstageAll,
                                unstageSelected),
                        stagedFiles),
                VBox(
                        ToolBar(StatusCountView(unstagedFiles),
                                spacer(),
                                update,
                                stageAll,
                                stageSelected),
                        unstagedFiles))
        files.styleClass += "files"

        content = SplitPane(files, fileDiff).addClass("working-copy-view")

        State.selectedRepositoryProperty().addListener { _, _, it ->
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
                val unstaged = (unstagedFiles.selectionModel.selectedItems + unstagedFiles.selectionModel.selectedItem).filterNotNull()

                stagedFiles.items.setAll(value.staged)
                unstagedFiles.items.setAll(value.unstaged)

                if (staged.isNotEmpty()) stagedFiles.selectionModel.selectIndices(-1,
                        *staged.map { stagedFiles.items.indexOf(it) }.toIntArray())
                if (unstaged.isNotEmpty()) unstagedFiles.selectionModel.selectIndices(-1,
                        *unstaged.map { unstagedFiles.items.indexOf(it) }.toIntArray())
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
