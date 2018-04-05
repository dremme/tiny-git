package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.TinyGit
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
import javafx.scene.layout.Priority
import javafx.stage.Window

private const val DEFAULT_STYLE_CLASS = "commit-dialog"

// TODO: show something on empty commit / merge commit
class CommitDialog(window: Window)
    : Dialog<Unit>(window, if (TinyGit.mergeService.isMerging.get()) I18N["dialog.commit.mergeTitle"] else I18N["dialog.commit.title"], true) {

    private val mergeService = TinyGit.mergeService
    private val commitService = TinyGit.commitService
    private val workingService = TinyGit.workingCopyService

    init {
        val files = FileStatusView(workingService.staged)
        files.prefWidth = 400.0
        files.prefHeight = 500.0
        Platform.runLater { files.selectionModel.selectFirst() }

        val message = textArea {
            promptText = I18N["dialog.commit.message"]
            prefHeight = 100.0
            textProperty().bindBidirectional(commitService.message)
            Platform.runLater { requestFocus() }
        }
        if (mergeService.isMerging.get()) commitService.setMergeMessage()

        val fileDiff = FileDiffView(files.selectionModel.selectedItemProperty())
        val amend = checkBox {
            text = I18N["dialog.commit.amend"]
            selectedProperty().addListener { _, _, it -> if (it) commitService.setHeadMessage() }
        }

        content = vbox {
            addClass(DEFAULT_STYLE_CLASS)

            +splitPane {
                vgrow(Priority.ALWAYS)
                +files
                +fileDiff
            }
            +message
            if (!mergeService.isMerging.get()) +amend
        }

        +DialogButton(DialogButton.ok(I18N["dialog.commit.button"]), message.textProperty().isEmpty)
        +DialogButton(DialogButton.CANCEL)

        focusAction = {
            workingService.status()
            fileDiff.refresh()
        }
        okAction = {
            commitService.commit(
                    message.text,
                    amend.isSelected,
                    { errorAlert(window, I18N["dialog.cannotCommit.header"], I18N["dialog.cannotCommit.text"]) })
        }
    }

}
