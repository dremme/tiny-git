package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.scene.control.Label
import javafx.scene.control.SplitPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class CommitDetailsView(repository: LocalRepository, commit: LocalCommit) : SplitPane() {

    init {
        styleClass += "commit-details-view"

        val pane = GridPane()
        pane.styleClass += "commit-details"
        pane.add(Label("Commit:"), 0, 0)
        pane.add(textField("${commit.id} [${commit.shortId}]", false), 1, 0)
        pane.add(Label("Parents:"), 0, 1)
        pane.add(textField(commit.parents.joinToString(), false), 1, 1)
        pane.add(Label("Author:"), 0, 2)
        pane.add(textField(commit.author, false), 1, 2)
        pane.add(Label("Date:"), 0, 3)
        pane.add(textField(commit.date.format(fullDate), false), 1, 3)
        pane.add(Label(" "), 0, 4)
        pane.add(textArea(commit.fullMessage, editable = false), 0, 5, 2, 1)

        val files = FileStatusView()
        files.items.addAll(LocalGit.diffTree(repository, commit.id))
        VBox.setVgrow(files, Priority.ALWAYS)

        items.addAll(pane, VBox(ToolBar(StatusCountView(files)), files))
    }

}
