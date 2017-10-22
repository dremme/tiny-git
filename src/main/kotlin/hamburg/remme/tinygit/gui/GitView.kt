package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Separator
import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import org.kordamp.ikonli.fontawesome.FontAwesome
import java.io.File

class GitView : VBox() {

    private val repositories = State.getRepositories()
    private val localRepositories = RepositoryView(repositories)
    private val overlay = StackPane(ProgressIndicator(-1.0))

    init {
        styleClass += "git-view"

        val toolBar = ToolBar(
                button("Add Working Copy",
                        icon(FontAwesome.DATABASE),
                        EventHandler {
                            val chooser = DirectoryChooser()
                            chooser.title = "Add Working Copy"
                            chooser.showDialog(this.scene.window)?.let {
                                if (File("${it.absolutePath}/.git").exists()) {
                                    State.addRepository(LocalRepository(it.absolutePath))
                                    localRepositories.selectionModel.selectLast()
                                }
                            }
                        }),
                Separator(),
                button("Commit",
                        icon(FontAwesome.PLUS),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { commit(State.getSelectedRepository()) }),
                Separator(),
                button("Push",
                        icon(FontAwesome.ARROW_UP),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { push(State.getSelectedRepository()) }),
                button("Pull",
                        icon(FontAwesome.ARROW_DOWN),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { pull(State.getSelectedRepository()) }),
                button("Fetch",
                        icon(FontAwesome.REFRESH),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { fetch(State.getSelectedRepository()) }),
                button("Tag",
                        icon(FontAwesome.TAG),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                Separator(),
                button("Branch",
                        icon(FontAwesome.CODE_FORK),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                button("Merge",
                        icon(FontAwesome.CODE_FORK).also { it.scaleY = -1.0 },
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                Separator(),
                button("Stash",
                        icon(FontAwesome.DOWNLOAD),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { stash(State.getSelectedRepository()) }),
                button("Apply Stash",
                        icon(FontAwesome.UPLOAD),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { stashApply(State.getSelectedRepository()) }),
                Separator(),
                button("Reset",
                        icon(FontAwesome.UNDO),
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }))

        val tabs = TabPane(LogView(), WorkingCopyView())
        val content = SplitPane(localRepositories, tabs)
        Platform.runLater { content.setDividerPosition(0, 0.20) }

        val info = StackPane(HBox(
                Label("Click "),
                Label("", icon(FontAwesome.CLOUD)),
                Label(" to add a working copy."))
                .also { it.styleClass += "box" })
        info.styleClass += "overlay"
        info.visibleProperty().bind(Bindings.isEmpty(repositories))

        overlay.styleClass += "progress-overlay"
        overlay.isVisible = false

        children.addAll(toolBar, StackPane(content, info, overlay).also { VBox.setVgrow(it, Priority.ALWAYS) })
    }

    private fun commit(repository: LocalRepository) {
        CommitDialog(repository, scene.window).show()
    }

    private fun fetch(repository: LocalRepository) {
        overlay.isVisible = true
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() {
                LocalGit.fetchPrune(repository)
            }

            override fun succeeded() {
                overlay.isVisible = false
                State.fireRefresh()
            }

            override fun failed() {
                overlay.isVisible = false
                exception.printStackTrace()
            }
        })
    }

    private fun pull(repository: LocalRepository) {
        overlay.isVisible = true
        State.cachedThreadPool.execute(object : Task<Boolean>() {
            override fun call(): Boolean {
                return LocalGit.pull(repository)
            }

            override fun succeeded() {
                overlay.isVisible = false
                if (value) State.fireRefresh()
            }

            override fun failed() {
                overlay.isVisible = false
                exception.printStackTrace()
            }
        })
    }

    private fun push(repository: LocalRepository) {
        overlay.isVisible = true
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() {
                LocalGit.push(repository)
            }

            override fun succeeded() {
                overlay.isVisible = false
                State.fireRefresh()
            }

            override fun failed() {
                overlay.isVisible = false
                exception.printStackTrace()
            }
        })
    }

    private fun stash(repository: LocalRepository) {
        overlay.isVisible = true
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() {
                LocalGit.stash(repository)
            }

            override fun succeeded() {
                overlay.isVisible = false
                State.fireRefresh()
            }

            override fun failed() {
                overlay.isVisible = false
                exception.printStackTrace()
            }
        })
    }

    private fun stashApply(repository: LocalRepository) {
        overlay.isVisible = true
        State.cachedThreadPool.execute(object : Task<Unit>() {
            override fun call() {
                LocalGit.stashPop(repository)
            }

            override fun succeeded() {
                overlay.isVisible = false
                State.fireRefresh()
            }

            override fun failed() {
                overlay.isVisible = false
                exception.printStackTrace()
            }
        })
    }

}
