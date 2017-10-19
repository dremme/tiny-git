package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import javafx.application.Platform
import javafx.collections.ListChangeListener
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
import java.io.File

class GitView : VBox() {

    private val repositories = State.getRepositories()
    private val localRepositories = RepositoryView(repositories)
    private val overlay = StackPane(ProgressIndicator(-1.0))

    init {
        styleClass += "git-view"

        val toolBar = ToolBar(
                button(icon = FontAwesome.database(),
                        tooltip = "Open working copy",
                        action = EventHandler {
                            val chooser = DirectoryChooser()
                            chooser.title = "Choose a Working Copy"
                            chooser.showDialog(this.scene.window)?.let {
                                if (File("${it.absolutePath}/.git").exists()) {
                                    State.addRepository(LocalRepository(it.absolutePath))
                                    localRepositories.selectionModel.selectLast()
                                }
                            }
                        }),
                Separator(),
                button(icon = FontAwesome.plus(),
                        tooltip = "New commit",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { commit(State.getSelectedRepository()) }),
                Separator(),
                button(icon = FontAwesome.arrowUp(),
                        tooltip = "Push",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { push(State.getSelectedRepository()) }),
                button(icon = FontAwesome.arrowDown(),
                        tooltip = "Pull",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { pull(State.getSelectedRepository()) }),
                button(icon = FontAwesome.refresh(),
                        tooltip = "Fetch and prune",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { fetch(State.getSelectedRepository()) }),
                button(icon = FontAwesome.tag(),
                        tooltip = "Tag",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                Separator(),
                button(icon = FontAwesome.codeFork(),
                        tooltip = "Branch",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                button(icon = FontAwesome.codeFork().also { it.scaleY = -1.0 },
                        tooltip = "Merge",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }),
                Separator(),
                button(icon = FontAwesome.download(),
                        tooltip = "Stash",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { stash(State.getSelectedRepository()) }),
                button(icon = FontAwesome.upload(),
                        tooltip = "Apply stash",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { stashApply(State.getSelectedRepository()) }),
                Separator(),
                button(icon = FontAwesome.undo(),
                        tooltip = "Soft reset",
                        disable = State.selectedRepositoryProperty().isNull.or(overlay.visibleProperty()),
                        action = EventHandler { }))

        val tabs = TabPane(LogView(), WorkingCopyView())
        val content = SplitPane(localRepositories, tabs)
        Platform.runLater { content.setDividerPosition(0, 0.20) }
        Platform.runLater { content.setDividerPosition(1, 0.40) }

        val info = StackPane(HBox(
                Label("Click "),
                Label("", FontAwesome.cloud()),
                Label(" to add a working copy."))
                .also { it.styleClass += "box" })
        info.styleClass += "overlay"
        info.isVisible = repositories.isEmpty()
        repositories.addListener(ListChangeListener { info.isVisible = it.list.isEmpty() })

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
