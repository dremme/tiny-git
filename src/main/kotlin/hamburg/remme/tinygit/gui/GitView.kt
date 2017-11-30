package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.git.api.PushRejectedException
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionCollection
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.flipXY
import hamburg.remme.tinygit.gui.builder.flipY
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.menuBar
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.dialog.AboutDialog
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Window
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.StashApplyFailureException
import java.io.File

class GitView : VBoxBuilder() {

    private val window: Window get() = scene.window
    private val repositoryView = RepositoryView()
    private val commitLog = CommitLogView()
    private val workingCopy = WorkingCopyView()
    private val tabs = TabPane(commitLog, workingCopy)

    init {
        addClass("git-view")

        // File
        val addCopy = Action("Add Repository", { FontAwesome.database() }, "Shortcut+O",
                handler = { addRepo() })
        val quit = Action("Quit TinyGit",
                handler = { Platform.exit() })
        // View
        val showCommits = Action("Show Commits", shortcut = "F1",
                handler = { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action("Show Working Copy", shortcut = "F2",
                handler = { tabs.selectionModel.select(workingCopy) })
        // Repository
        val commit = Action("Commit", { FontAwesome.plus() }, "Shortcut+Plus", State.canCommit.not(),
                { commit(State.selectedRepository) })
        val push = Action("Push", { FontAwesome.cloudUpload() }, "Shortcut+P", State.canPush.not(),
                { push(State.selectedRepository, false) }, State.aheadProperty())
        val pushForce = Action("Force Push", { FontAwesome.cloudUpload() }, "Shortcut+Shift+P", State.canPush.not(),
                { push(State.selectedRepository, true) })
        val pull = Action("Pull", { FontAwesome.cloudDownload() }, "Shortcut+L", State.canPull.not(),
                { pull(State.selectedRepository) }, State.behindProperty())
        val fetch = Action("Fetch", { FontAwesome.refresh() }, "Shortcut+F", State.canFetch.not(),
                { fetch(State.selectedRepository) })
        val tag = Action("Tag", { FontAwesome.tag() }, "Shortcut+T", State.canTag.not(),
                handler = { /* TODO */ })
        val branch = Action("Branch", { FontAwesome.codeFork() }, "Shortcut+B", State.canBranch.not(),
                { createBranch(State.selectedRepository) })
        val merge = Action("Merge", { FontAwesome.codeFork().flipY() }, "Shortcut+M", State.canMerge.not(),
                handler = { /* TODO */ })
        val stash = Action("Stash", { FontAwesome.cube() }, "Shortcut+S", State.canStash.not(),
                { stash(State.selectedRepository) })
        val stashPop = Action("Pop Stash", { FontAwesome.cube().flipXY() }, "Shortcut+Shift+S", State.canApplyStash.not(),
                { stashPop(State.selectedRepository) })
        val reset = Action("Auto Reset", { FontAwesome.undo() }, disable = State.canReset.not(),
                handler = { autoReset(State.selectedRepository) })
        val squash = Action("Auto Squash", { FontAwesome.gavel() }, disable = State.canSquash.not(),
                handler = { /* TODO */ })
        val settings = Action("Settings", { FontAwesome.cog() }, disable = State.canSettings.not(),
                handler = { SettingsDialog(State.selectedRepository, window).show() })
        // ?
        val github = Action("Star TinyGit on GitHub", { FontAwesome.githubAlt() },
                handler = { TinyGit.show("https://github.com/deso88/TinyGit") })
        val about = Action("About",
                handler = { AboutDialog(window).show() })

        +menuBar {
            isUseSystemMenuBar = true
            +ActionCollection("File", ActionGroup(addCopy), ActionGroup(quit))
            +ActionCollection("View", ActionGroup(showCommits, showWorkingCopy))
            +ActionCollection("Repository",
                    ActionGroup(commit),
                    ActionGroup(push, pushForce, pull, fetch, tag),
                    ActionGroup(branch, merge),
                    ActionGroup(stash, stashPop),
                    ActionGroup(reset, squash),
                    ActionGroup(settings))
            +ActionCollection("Actions", *workingCopy.actions)
            +ActionCollection("?", ActionGroup(github, about))
        }
        +toolBar {
            +ActionGroup(addCopy)
            +ActionGroup(commit, push, pull, fetch, tag)
            +ActionGroup(branch, merge)
            +ActionGroup(stash, stashPop)
            +ActionGroup(reset, squash)
        }
        +stackPane {
            vgrow(Priority.ALWAYS)
            +splitPane {
                addClass("content")
                +repositoryView
                +tabs
                Platform.runLater { setDividerPosition(0, 0.20) }
            }
            +stackPane {
                addClass("overlay")
                visibleWhen(State.showGlobalInfo)
                +hbox {
                    addClass("box")
                    +Text("Click ")
                    +FontAwesome.database()
                    +Text(" to add a repository.")
                }
            }
            +stackPane {
                addClass("progress-overlay")
                visibleWhen(State.showGlobalOverlay)
                +ProgressIndicator(-1.0)
                +label { textProperty().bind(State.processTextProperty()) }
            }
        }
    }

    private fun addRepo() {
        directoryChooser(window, "Add Repository") {
            if (File("${it.absolutePath}/.git").exists()) {
                val repository = LocalRepository(it.absolutePath)
                if (State.repositories.none { it.path == repository.path }) {
                    State.repositories += LocalRepository(it.absolutePath)
                }
            } else {
                errorAlert(window, "Invalid Repository",
                        "'${it.absolutePath}' does not contain a valid '.git' directory.")
            }
        }
    }

    private fun commit(repository: LocalRepository) {
        CommitDialog(repository, window).show()
    }

    private fun fetch(repository: LocalRepository) {
        State.addProcess("Fetching...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.fetchPrune(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun pull(repository: LocalRepository) {
        State.addProcess("Pulling commits...")
        State.execute(object : Task<Boolean>() {
            override fun call() = Git.pull(repository)

            override fun succeeded() {
                if (value) State.fireRefresh()
            }

            override fun failed() {
                exception.printStackTrace()
                // TODO: make more specific to exception
                errorAlert(window, "Cannot Pull From Remote Branch",
                        "${exception.message}\n\nPlease commit or stash them before pulling.")
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        if (!Git.hasRemote(repository)) {
            errorAlert(window, "Push", "No remote configured.")
            SettingsDialog(repository, window).show()
            return
        }

        if (force && !confirmWarningAlert(window, "Force Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) return

        State.addProcess("Pushing commits...")
        State.execute(object : Task<Unit>() {
            override fun call() {
                if (force) Git.pushForce(repository)
                else Git.push(repository)
            }

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is PushRejectedException -> errorAlert(window, "Cannot Push to Remote Branch",
                            "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.")
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun createBranch(repository: LocalRepository) {
        textInputDialog(window, "Enter a New Branch Name", FontAwesome.codeFork()) { name ->
            State.addProcess("Branching...")
            State.execute(object : Task<Unit>() {
                override fun call() = Git.branchCreate(repository, name)

                override fun succeeded() = State.fireRefresh()

                override fun failed() {
                    when (exception) {
                        is RefAlreadyExistsException -> errorAlert(window, "Cannot Create Branch",
                                "Branch '$name' does already exist in the working copy.")
                        else -> exception.printStackTrace()
                    }
                }

                override fun done() = Platform.runLater { State.removeProcess() }
            })
        }
    }

    private fun stash(repository: LocalRepository) {
        State.addProcess("Stashing files...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.stash(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun stashPop(repository: LocalRepository) {
        State.addProcess("Applying stash...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.stashPop(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is StashApplyFailureException -> {
                        State.fireRefresh()
                        errorAlert(window, "Cannot Pop Stash",
                                "Applying stashed changes resulted in a conflict.\nTherefore the stash entry has been preserved.")
                    }
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun autoReset(repository: LocalRepository) {
        if (!confirmWarningAlert(window, "Auto Reset Branch",
                "This will automatically reset the current branch to its remote branch.\nUnpushed commits will be lost.")) return

        State.addProcess("Resetting branch...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.resetHard(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

}
