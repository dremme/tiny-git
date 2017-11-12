package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.PushRejectedException
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Separator
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.SplitPane
import javafx.scene.control.TabPane
import javafx.scene.control.ToolBar
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import java.awt.Desktop
import java.io.File
import java.net.URI

class GitView : VBox() {

    init {
        styleClass += "git-view"

        val addCopy = Action("Add Working Copy", FontAwesome.database(), "Shortcut+O", action = EventHandler { addCopy() })
        val settings = Action("Settings", FontAwesome.cog(), action = EventHandler { SettingsDialog(State.getSelectedRepository(), scene.window).show() })
        val commit = Action("Commit", FontAwesome.plus(), "Shortcut+Plus", State.canCommit.not(), EventHandler { commit(State.getSelectedRepository()) })
        val push = Action("Push", FontAwesome.cloudUpload(), "Shortcut+P", State.canPush.not(), EventHandler { push(State.getSelectedRepository(), false) })
        val pushForce = Action("Force Push", FontAwesome.cloudUpload(), "Shortcut+Shift+P", State.canPush.not(), EventHandler { push(State.getSelectedRepository(), true) })
        val pull = Action("Pull", FontAwesome.cloudDownload(), "Shortcut+L", State.canPull.not(), EventHandler { pull(State.getSelectedRepository()) })
        val fetch = Action("Fetch", FontAwesome.refresh(), "Shortcut+F", State.canFetch.not(), EventHandler { fetch(State.getSelectedRepository()) })
        val createBranch = Action("Tag", FontAwesome.tag(), "Shortcut+T", State.canTag.not(), EventHandler { createBranch(State.getSelectedRepository()) })
        val stash = Action("Branch", FontAwesome.codeFork(), "Shortcut+B", State.canBranch.not(), EventHandler { stash(State.getSelectedRepository()) })
        val stashApply = Action("Merge", FontAwesome.codeFork().flipY(), "Shortcut+M", State.canMerge.not(), EventHandler { stashApply(State.getSelectedRepository()) })
        val github = Action("Star TinyGit on GitHub", FontAwesome.githubAlt(), action = EventHandler { if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI("https://github.com/deso88/TinyGit")) })

        //<editor-fold desc="MenuBar">
        val menuBar = MenuBar(
                Menu("File", null,
                        menuItem("Add Working Copy",
                                FontAwesome.database(),
                                shortcut = "Shortcut+O",
                                action = addCopy),
                        SeparatorMenuItem(),
                        menuItem("Quit TinyGit",
                                action = EventHandler { Platform.exit() })),
                Menu("Repository", null,
                        menuItem("Settings",
                                FontAwesome.cog(),
                                action = EventHandler { SettingsDialog(State.getSelectedRepository(), scene.window).show() })),
                Menu("Actions", null,
                        menuItem("Commit",
                                FontAwesome.plus(),
                                shortcut = "Shortcut+Plus",
                                disable = State.canCommit.not(),
                                action = commit),
                        SeparatorMenuItem(),
                        menuItem("Push",
                                FontAwesome.cloudUpload(),
                                shortcut = "Shortcut+P",
                                disable = State.canPush.not(),
                                action = push),
                        menuItem("Force Push",
                                FontAwesome.cloudUpload(),
                                shortcut = "Shortcut+Shift+P",
                                disable = State.canPush.not(),
                                action = pushForce),
                        menuItem("Pull",
                                FontAwesome.cloudDownload(),
                                disable = State.canPull.not(),
                                shortcut = "Shortcut+L",
                                action = pull),
                        menuItem("Fetch",
                                FontAwesome.refresh(),
                                shortcut = "Shortcut+F",
                                disable = State.canFetch.not(),
                                action = fetch),
                        menuItem("Tag",
                                FontAwesome.tag(),
                                shortcut = "Shortcut+T",
                                disable = State.canTag.not(),
                                action = EventHandler { }),
                        SeparatorMenuItem(),
                        menuItem("Branch",
                                FontAwesome.codeFork(),
                                shortcut = "Shortcut+B",
                                disable = State.canBranch.not(),
                                action = createBranch),
                        menuItem("Merge",
                                FontAwesome.codeFork().flipY(),
                                shortcut = "Shortcut+M",
                                disable = State.canMerge.not(),
                                action = EventHandler { }),
                        SeparatorMenuItem(),
                        menuItem("Stash",
                                FontAwesome.cube(),
                                shortcut = "Shortcut+S",
                                disable = State.canStash.not(),
                                action = stash),
                        menuItem("Apply Stash",
                                FontAwesome.cube(),
                                shortcut = "Shortcut+Shift+S",
                                disable = State.canApplyStash.not(),
                                action = stashApply),
                        SeparatorMenuItem(),
                        menuItem("Reset",
                                FontAwesome.undo(),
                                shortcut = "Shortcut+R",
                                disable = State.canReset.not(),
                                action = EventHandler { })),
                Menu("?", null,
                        menuItem("Star TinyGit on GitHub",
                                FontAwesome.githubAlt(),
                                action = github),
                        menuItem("About TinyGit",
                                action = EventHandler { })))
        menuBar.isUseSystemMenuBar = true
        //</editor-fold>

        //<editor-fold desc="ToolBar">
        val toolBar = ToolBar(
                button("Add Working Copy",
                        FontAwesome.database(),
                        addCopy),
                Separator(),
                button("Commit",
                        FontAwesome.plus(),
                        disable = State.canCommit.not(),
                        action = commit),
                Separator(),
                button("Push",
                        FontAwesome.cloudUpload(),
                        disable = State.canPush.not(),
                        action = push),
                button("Pull",
                        FontAwesome.cloudDownload(),
                        disable = State.canPull.not(),
                        action = pull),
                button("Fetch",
                        FontAwesome.refresh(),
                        disable = State.canFetch.not(),
                        action = fetch),
                button("Tag",
                        FontAwesome.tag(),
                        disable = State.canTag.not(),
                        action = EventHandler { }),
                Separator(),
                button("Branch",
                        FontAwesome.codeFork(),
                        disable = State.canBranch.not(),
                        action = createBranch),
                button("Merge",
                        FontAwesome.codeFork().flipY(),
                        disable = State.canMerge.not(),
                        action = EventHandler { }),
                Separator(),
                button("Stash",
                        FontAwesome.cube(),
                        disable = State.canStash.not(),
                        action = stash),
                button("Apply Stash",
                        FontAwesome.cube(),
                        disable = State.canApplyStash.not(),
                        action = stashApply),
                Separator(),
                button("Reset",
                        FontAwesome.undo(),
                        disable = State.canReset.not(),
                        action = EventHandler { }))
        //</editor-fold>

        val tabs = TabPane(LogView(), WorkingCopyView())
        val content = SplitPane(RepositoryView(), tabs)
        Platform.runLater { content.setDividerPosition(0, 0.20) }

        val info = StackPane(HBox(
                Label("Click "),
                Label("", FontAwesome.database()),
                Label(" to add a working copy."))
                .addClass("box"))
        info.styleClass += "overlay"
        info.visibleProperty().bind(State.showGlobalInfo)

        val overlay = StackPane(
                ProgressIndicator(-1.0),
                Label().also { it.textProperty().bind(State.processTextProperty()) })
        overlay.styleClass += "progress-overlay"
        overlay.visibleProperty().bind(State.showGlobalOverlay)

        children.addAll(
                menuBar,
                toolBar,
                StackPane(content, info, overlay).also { VBox.setVgrow(it, Priority.ALWAYS) })
    }

    private fun addCopy() {
        val chooser = DirectoryChooser()
        chooser.title = "Add Working Copy"
        chooser.showDialog(this.scene.window)?.let {
            if (File("${it.absolutePath}/.git").exists()) {
                val repository = LocalRepository(it.absolutePath)
                if (State.getRepositories().none { it.path == repository.path }) {
                    State.addRepository(LocalRepository(it.absolutePath))
                }
            } else {
                errorAlert(scene.window,
                        "Invalid Working Copy",
                        "${it.absolutePath}\ndoes not contain a valid '.git' directory.")
            }
        }
    }

    private fun commit(repository: LocalRepository) {
        CommitDialog(repository, scene.window).show()
    }

    private fun fetch(repository: LocalRepository) {
        State.addProcess("Fetching...")
        State.execute(object : Task<Unit>() {
            override fun call() = LocalGit.fetchPrune(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = State.removeProcess()
        })
    }

    private fun pull(repository: LocalRepository) {
        State.addProcess("Pulling commits...")
        State.execute(object : Task<Boolean>() {
            override fun call() = LocalGit.pull(repository)

            override fun succeeded() {
                if (value) State.fireRefresh()
            }

            override fun failed() {
                exception.printStackTrace()
                // TODO: make more specific to exception
                errorAlert(scene.window,
                        "Cannot Pull From Remote Branch",
                        "${exception.message}\n\nPlease commit or stash them before pulling.")
            }

            override fun done() = State.removeProcess()
        })
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        if (force && !confirmWarningAlert(scene.window,
                "Force Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) return

        State.addProcess("Pushing commits...")
        State.execute(object : Task<Unit>() {
            override fun call() {
                if (force) LocalGit.pushForce(repository)
                else LocalGit.push(repository)
            }

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is PushRejectedException -> errorAlert(scene.window,
                            "Cannot Push to Remote Branch",
                            "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.")
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = State.removeProcess()
        })
    }

    private fun createBranch(repository: LocalRepository) {
        textInputDialog(scene.window, FontAwesome.codeFork())?.let { name ->
            State.addProcess("Branching...")
            State.execute(object : Task<Unit>() {
                override fun call() = LocalGit.branchCreate(repository, name)

                override fun succeeded() = State.fireRefresh()

                override fun failed() {
                    when (exception) {
                        is RefAlreadyExistsException -> errorAlert(scene.window,
                                "Cannot Create Branch",
                                "Branch '$name' does already exist in the working copy.")
                        else -> exception.printStackTrace()
                    }
                }

                override fun done() = State.removeProcess()
            })
        }
    }

    private fun stash(repository: LocalRepository) {
        State.addProcess("Stashing files...")
        State.execute(object : Task<Unit>() {
            override fun call() = LocalGit.stash(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = State.removeProcess()
        })
    }

    private fun stashApply(repository: LocalRepository) {
        State.addProcess("Applying stash...")
        State.execute(object : Task<Unit>() {
            override fun call() = LocalGit.stashPop(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = State.removeProcess()
        })
    }

}
