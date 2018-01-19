package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.domain.service.CommitService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.WorkingCopyService
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
    : Dialog<Unit>(window, if (MergeService.isMerging.get()) "Merge Commit" else "New Commit", true) {

    init {
        val repository = RepositoryService.activeRepository.get()!!

        val files = FileStatusView(WorkingCopyService.staged)
        files.prefWidth = 400.0
        files.prefHeight = 500.0

        val message = textArea {
            promptText = "Enter commit message"
            prefHeight = 100.0
            textProperty().bindBidirectional(WorkingCopyService.message)
            Platform.runLater { requestFocus() }
        }
        if (MergeService.isMerging.get() && message.text.isNullOrBlank()) message.text = gitMergeMessage(repository)

        val amend = checkBox {
            text = "Amend last commit."
            selectedProperty().addListener { _, _, it ->
                if (it && message.text.isNullOrBlank()) message.text = gitHeadMessage(repository)
            }
        }

        +DialogButton(DialogButton.ok("Commit"),
                message.textProperty().isEmpty.or(Bindings.isEmpty(files.items)))
        +DialogButton(DialogButton.CANCEL)

        // TODO: no focus refresh
//        focusAction = {
//             TODO: ugly
//            WorkingCopyService.status {
//                files.selectionModel.selectedItem?.let { fileDiff.update() } ?: files.selectionModel.selectFirst()
//            }
//        }
        okAction = {
            CommitService.commit(
                    message.text,
                    amend.isSelected,
                    { errorAlert(window, "Cannot Commit", "Cannot commit because there are unmerged changes.") })
        }
        content = vbox {
            addClass("commit-view")

            +splitPane {
                vgrow(Priority.ALWAYS)
                +files
                +FileDiffView(files.selectionModel.selectedItemProperty())
            }
            +message
            if (!MergeService.isMerging.get()) +amend
        }
    }

}
