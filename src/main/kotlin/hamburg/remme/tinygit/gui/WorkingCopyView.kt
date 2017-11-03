package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStatus
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.kordamp.ikonli.fontawesome.FontAwesome

class WorkingCopyView : Tab() {

    private val stagedFiles = FileStatusView()
    private val unstagedFiles = FileStatusView()
    private val fileDiff = FileDiffView()
    private var task: Task<*>? = null

    init {
        text = "Files"
        graphic = icon(FontAwesome.DESKTOP)
        isClosable = false

        val unstageAll = button("Unstage all",
                action = EventHandler { unstage(State.getSelectedRepository()!!) })
        val unstageSelected = button("Unstage selected",
                action = EventHandler { unstage(State.getSelectedRepository()!!, stagedFiles.selectionModel.selectedItems) })
        unstageAll.disableProperty().bind(Bindings.isEmpty(stagedFiles.items))
        unstageSelected.disableProperty().bind(Bindings.isEmpty(stagedFiles.selectionModel.selectedItems))
        stagedFiles.selectionModel.selectedItems.addListener(ListChangeListener {
            if (it.list.isNotEmpty()) {
                fileDiff.update(State.getSelectedRepository()!!, stagedFiles.selectionModel.selectedItem)
                unstagedFiles.selectionModel.clearSelection()
            }
        })
        val stagedTools = ToolBar(
                StatusCountView(stagedFiles),
                spacer(),
                unstageAll,
                unstageSelected)

        val update = button("Update all",
                action = EventHandler { update(State.getSelectedRepository()!!) })
        val stageAll = button("Stage all",
                action = EventHandler { stage(State.getSelectedRepository()!!) })
        val stageSelected = button("Stage selected",
                action = EventHandler { stage(State.getSelectedRepository()!!, unstagedFiles.selectionModel.selectedItems) })
        update.disableProperty().bind(Bindings.isEmpty(unstagedFiles.items))
        stageAll.disableProperty().bind(Bindings.isEmpty(unstagedFiles.items))
        stageSelected.disableProperty().bind(Bindings.isEmpty(unstagedFiles.selectionModel.selectedItems))
        unstagedFiles.selectionModel.selectedItems.addListener(ListChangeListener {
            if (it.list.isNotEmpty()) {
                fileDiff.update(State.getSelectedRepository()!!, unstagedFiles.selectionModel.selectedItem)
                stagedFiles.selectionModel.clearSelection()
            }
        })
        val unstagedTools = ToolBar(
                StatusCountView(unstagedFiles),
                spacer(),
                update,
                stageAll,
                stageSelected)

        VBox.setVgrow(stagedFiles, Priority.ALWAYS)
        VBox.setVgrow(unstagedFiles, Priority.ALWAYS)

        val files = SplitPane(VBox(stagedTools, stagedFiles), VBox(unstagedTools, unstagedFiles))
        files.styleClass += "files"

        val pane = SplitPane(files, fileDiff)
        pane.styleClass += "working-copy-view"
        content = pane

        State.selectedRepositoryProperty().addListener { _, _, it ->
            fileDiff.clear()
            fetchFiles(it)
        }
        State.addFocusListener {
            fileDiff.clear()
            fetchCurrent()
        }
    }

    private fun fetchCurrent() {
        State.getSelectedRepository()?.let { fetchFiles(it) }
    }

    private fun fetchFiles(repository: LocalRepository) {
        println("Status for working copy: $repository")
        task?.cancel()
        task = object : Task<LocalStatus>() {
            val stagedSelected = stagedFiles.selectionModel.selectedIndex >= 0
            val selected = if (stagedSelected) stagedFiles.selectionModel.selectedItem else unstagedFiles.selectionModel.selectedItem

            override fun call() = LocalGit.status(repository)// TODO: git status might not be good enough here

            override fun succeeded() {
                stagedFiles.items.setAll(value.staged)
                unstagedFiles.items.setAll(value.unstaged)

                if (stagedSelected) stagedFiles.items.find { it == selected }?.let { stagedFiles.selectionModel.select(it) }
                else unstagedFiles.items.find { it == selected }?.let { unstagedFiles.selectionModel.select(it) }
            }
        }
        State.cachedThreadPool.execute(task)
    }

    private fun stage(repository: LocalRepository) {
        LocalGit.addAll(repository)
        fetchFiles(repository)
    }

    private fun stage(repository: LocalRepository, files: List<LocalFile>) {
        LocalGit.add(repository, files)
        fetchFiles(repository)
    }

    private fun update(repository: LocalRepository) {
        LocalGit.addAllUpdate(repository)
        fetchFiles(repository)
    }

    private fun unstage(repository: LocalRepository) {
        LocalGit.reset(repository)
        fetchFiles(repository)
    }

    private fun unstage(repository: LocalRepository, files: List<LocalFile>) {
        LocalGit.reset(repository, files)
        fetchFiles(repository)
    }

}
