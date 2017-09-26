package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.control.SplitPane
import javafx.scene.control.Tab
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class WorkingCopyView : Tab() {

    private val stagedFiles = FileStatusView()
    private val unstagedFiles = FileStatusView()
    private val fileDiff = FileDiffView()

    init {
        text = "Files"
        graphic = FontAwesome.desktop()
        isClosable = false

        val unstageAll = button("Unstage all",
                action = EventHandler { unstage(State.getSelectedRepository()) })
        val unstageSelected = button("Unstage selected",
                action = EventHandler { unstage(State.getSelectedRepository(), stagedFiles.selectionModel.selectedItems) })
        unstageAll.isDisable = true
        unstageSelected.isDisable = true
        stagedFiles.items.addListener(ListChangeListener { unstageAll.isDisable = it.list.isEmpty() })
        stagedFiles.selectionModel.selectedItems.addListener(ListChangeListener {
            unstageSelected.isDisable = it.list.isEmpty()
            if (it.list.isNotEmpty()) {
                fileDiff.setFile(State.getSelectedRepository(), stagedFiles.selectionModel.selectedItem)
                unstagedFiles.selectionModel.clearSelection()
            }
        })
        val stagedTools = ToolBar(
                StatusCountView(stagedFiles),
                spacer(),
                unstageAll,
                unstageSelected)

        val update = button("Update all",
                action = EventHandler { update(State.getSelectedRepository()) })
        val stageAll = button("Stage all",
                action = EventHandler { stage(State.getSelectedRepository()) })
        val stageSelected = button("Stage selected",
                action = EventHandler { stage(State.getSelectedRepository(), unstagedFiles.selectionModel.selectedItems) })
        update.isDisable = true
        stageAll.isDisable = true
        stageSelected.isDisable = true
        unstagedFiles.items.addListener(ListChangeListener {
            update.isDisable = it.list.isEmpty()
            stageAll.isDisable = it.list.isEmpty()
        })
        unstagedFiles.selectionModel.selectedItems.addListener(ListChangeListener {
            stageSelected.isDisable = it.list.isEmpty()
            if (it.list.isNotEmpty()) {
                fileDiff.setFile(State.getSelectedRepository(), unstagedFiles.selectionModel.selectedItem)
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
            fileDiff.clearFile()
            fetchFiles(it)
        }
        State.addRefreshListener {
            fileDiff.clearFile()
            fetchCurrent()
        }
    }

    private fun fetchCurrent() {
        if (State.hasSelectedRepository()) fetchFiles(State.getSelectedRepository())
    }

    private fun fetchFiles(repository: LocalRepository) {
        println("Status for working copy: $repository")
        val stagedSelected = stagedFiles.selectionModel.selectedIndex >= 0
        val selected = if (stagedSelected) stagedFiles.selectionModel.selectedItem else unstagedFiles.selectionModel.selectedItem

        val status = LocalGit.status(repository) // TODO: git status might not be good enough here
        stagedFiles.items.setAll(status.staged)
        unstagedFiles.items.setAll(status.unstaged)

        if (stagedSelected) stagedFiles.selectionModel.select(selected)
        else unstagedFiles.selectionModel.select(selected)
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
        LocalGit.updateAll(repository)
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
