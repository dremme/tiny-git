package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.git.gitHeadMessage
import hamburg.remme.tinygit.git.gitMergeMessage
import hamburg.remme.tinygit.gui.FileDiffView
import hamburg.remme.tinygit.gui.FileStatusView
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.checkBox
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.textArea
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.layout.Priority
import javafx.stage.Window

class CommitDialog(window: Window)
    : Dialog<Unit>(window, if (TinyGit.mergeService.isMerging.get()) "Merge Commit" else "New Commit", true) {

    private val repoService = TinyGit.repositoryService
    private val mergeService = TinyGit.mergeService
    private val commitService = TinyGit.commitService
    private val workingService = TinyGit.workingCopyService

    init {

        val files = FileStatusView(workingService.staged)
        files.prefWidth = 400.0
        files.prefHeight = 500.0
        Platform.runLater { files.selectionModel.selectFirst() }

        val message = textArea {
            promptText = "Enter commit message"
            prefHeight = 100.0
            textProperty().bindBidirectional(workingService.message)
            Platform.runLater { requestFocus() }
        }
        if (mergeService.isMerging.get() && message.text.isNullOrBlank()) message.text = gitMergeMessage(repoService.activeRepository.get()!!)

        val fileDiff = FileDiffView(files.selectionModel.selectedItemProperty())
        val amend = checkBox {
            text = "Amend last commit."
            selectedProperty().addListener { _, _, it ->
                if (it && message.text.isNullOrBlank()) message.text = gitHeadMessage(repoService.activeRepository.get()!!)
            }
        }

        +DialogButton(DialogButton.ok("Commit"),
                message.textProperty().isEmpty.or(Bindings.isEmpty(files.items)))
        +DialogButton(DialogButton.CANCEL)

        focusAction = { fileDiff.refresh() }
        okAction = {
            commitService.commit(
                    message.text,
                    amend.isSelected,
                    { errorAlert(window, "Cannot Commit", "Cannot commit because there are unmerged changes.") })
        }
        content = vbox {
            addClass("commit-view")

            +splitPane {
                vgrow(Priority.ALWAYS)
                +files
                +fileDiff
            }
            +message
            if (!mergeService.isMerging.get()) +amend
        }
    }

}
