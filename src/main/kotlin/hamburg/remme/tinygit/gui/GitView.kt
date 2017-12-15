package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.git.api.PrepareSquashException
import hamburg.remme.tinygit.git.api.PushRejectedException
import hamburg.remme.tinygit.git.api.SquashException
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionCollection
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.choiceDialog
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.directoryChooser
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.flipX
import hamburg.remme.tinygit.gui.builder.flipXY
import hamburg.remme.tinygit.gui.builder.flipY
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.menuBar
import hamburg.remme.tinygit.gui.builder.progressSpinner
import hamburg.remme.tinygit.gui.builder.splitPane
import hamburg.remme.tinygit.gui.builder.stackPane
import hamburg.remme.tinygit.gui.builder.textAreaDialog
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.builder.toolBar
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.dialog.AboutDialog
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Window
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.StashApplyFailureException
import org.eclipse.jgit.api.errors.UnmergedPathsException

class GitView : VBoxBuilder() {

    private val window: Window get() = scene.window
    private val repositoryView = RepositoryView()
    private val commitLog = CommitLogView()
    private val workingCopy = WorkingCopyView()
    private val tabs = TabPane(commitLog, workingCopy)

    init {
        addClass("git-view")

        // File
        val newCopy = Action("New Repository", { FontAwesome.folder() }, "Shortcut+N",
                handler = { newRepo() })
        val addCopy = Action("Add Repository", { FontAwesome.folderOpen() }, "Shortcut+O",
                handler = { addRepo() })
        val quit = Action("Quit TinyGit", { FontAwesome.signOut() },
                handler = { Platform.exit() })
        // View
        val showCommits = Action("Show Commits", { FontAwesome.list() }, "F1",
                handler = { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action("Show Working Copy", { FontAwesome.hdd() }, "F2",
                handler = { tabs.selectionModel.select(workingCopy) })
        // Repository
        val commit = Action("Commit", { FontAwesome.plus() }, "Shortcut+K", State.canCommit.not(),
                { commit(State.getSelectedRepository()) })
        val push = Action("Push", { FontAwesome.cloudUpload() }, "Shortcut+P", State.canPush.not(),
                { push(State.getSelectedRepository(), false) }, State.ahead)
        val pushForce = Action("Force Push", { FontAwesome.cloudUpload() }, "Shortcut+Shift+P", State.canPush.not(),
                { push(State.getSelectedRepository(), true) }, State.ahead)
        val pull = Action("Pull", { FontAwesome.cloudDownload() }, "Shortcut+L", State.canPull.not(),
                { pull(State.getSelectedRepository()) }, State.behind)
        val fetch = Action("Fetch", { FontAwesome.refresh() }, "Shortcut+F", State.canFetch.not(),
                { fetch(State.getSelectedRepository()) })
        val fetchGc = Action("Fetch and GC", { FontAwesome.eraser() }, "Shortcut+Shift+F", State.canFetch.not(),
                { fetchGc(State.getSelectedRepository()) })
        val tag = Action("Tag", { FontAwesome.tag() }, "Shortcut+T", State.canTag.not(),
                handler = { /* TODO */ })
        val branch = Action("Branch", { FontAwesome.codeFork() }, "Shortcut+B", State.canBranch.not(),
                { createBranch(State.getSelectedRepository()) })
        val merge = Action("Merge", { FontAwesome.codeFork().flipY() }, "Shortcut+M", State.canMerge.not(),
                handler = { /* TODO */ })
        val rebase = Action("Rebase", { FontAwesome.levelUp().flipX() }, "Shortcut+R", State.canRebase.not(),
                handler = { rebase(State.getSelectedRepository()) })
        val rebaseContinue = Action("Continue Rebase", { FontAwesome.forward() }, "Shortcut+Shift+R", State.canRebaseContinue.not(),
                handler = { rebaseContinue(State.getSelectedRepository()) })
        val rebaseAbort = Action("Abort Rebase", { FontAwesome.timesCircle() }, disable = State.canRebaseAbort.not(),
                handler = { rebaseAbort(State.getSelectedRepository()) })
        val stash = Action("Stash", { FontAwesome.cube() }, "Shortcut+S", State.canStash.not(),
                { stash(State.getSelectedRepository()) })
        val stashPop = Action("Pop Stash", { FontAwesome.cube().flipXY() }, "Shortcut+Shift+S", State.canApplyStash.not(),
                { stashPop(State.getSelectedRepository()) })
        val reset = Action("Auto-Reset", { FontAwesome.undo() }, disable = State.canReset.not(),
                handler = { autoReset(State.getSelectedRepository()) })
        val squash = Action("Auto-Squash", { FontAwesome.gavel() }, disable = State.canSquash.not(),
                handler = { autoSquash(State.getSelectedRepository()) })
        // ?
        val github = Action("Star TinyGit on GitHub", { FontAwesome.github() },
                handler = { TinyGit.show("https://github.com/deso88/TinyGit") })
        val about = Action("About", { FontAwesome.questionCircle() },
                handler = { AboutDialog(window).show() })

        +menuBar {
            isUseSystemMenuBar = true
            +ActionCollection("File", ActionGroup(newCopy, addCopy), ActionGroup(quit))
            +ActionCollection("View", ActionGroup(showCommits, showWorkingCopy))
            +ActionCollection("Repository",
                    ActionGroup(commit),
                    ActionGroup(push, pushForce, pull, fetch, fetchGc, tag),
                    ActionGroup(branch, merge),
                    ActionGroup(rebase, rebaseContinue, rebaseAbort),
                    ActionGroup(stash, stashPop),
                    ActionGroup(reset, squash),
                    *repositoryView.actions)
            +ActionCollection("Actions", *workingCopy.actions)
            +ActionCollection("?", ActionGroup(github, about))
        }
        +toolBar {
            visibleWhen(State.showToolBar)
            managedWhen(State.showToolBar)
            +ActionGroup(addCopy)
            +ActionGroup(commit, push, pull, fetch, tag)
            +ActionGroup(branch, merge)
            +ActionGroup(stash, stashPop)
            +ActionGroup(reset, squash)
        }
        +toolBar {
            visibleWhen(State.showRebaseBar)
            managedWhen(State.showRebaseBar)
            +ActionGroup(addCopy)
            +ActionGroup(rebaseContinue, rebaseAbort)
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
                    +FontAwesome.folderOpen()
                    +Text(" to add a repository.")
                }
            }
            +stackPane {
                addClass("progress-overlay")
                visibleWhen(State.showGlobalOverlay)
                +progressSpinner {}
                +label { textProperty().bind(State.processTextProperty()) }
            }
        }
    }

    private fun newRepo() {
        directoryChooser(window, "New Repository") {
            State.addRepository(Git.init(it))
        }
    }

    private fun addRepo() {
        directoryChooser(window, "Add Repository") {
            if ("${it.absolutePath}/.git".asPath().exists()) {
                State.addRepository(LocalRepository(it.absolutePath))
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
        State.startProcess("Fetching...", object : Task<Unit>() {
            override fun call() = Git.fetch(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun fetchGc(repository: LocalRepository) {
        State.startProcess("Fetching and GC...", object : Task<Unit>() {
            override fun call() = Git.fetchGc(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun pull(repository: LocalRepository) {
        State.startProcess("Pulling commits...", object : Task<Boolean>() {
            override fun call() = Git.pull(repository)

            override fun succeeded() {
                if (value) State.fireRefresh(this)
            }

            override fun failed() {
                when (exception) {
                    is CheckoutConflictException -> errorAlert(window, "Cannot Pull From Remote Branch",
                            "${exception.message}\n\nPlease commit or stash them before pulling.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        if (!Git.hasRemote(repository)) {
            errorAlert(window, "Push", "No remote configured.")
            SettingsDialog(repository, window).show()
            return
        }

        if (force && !confirmWarningAlert(window, "Force Push", "Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) return

        State.startProcess("Pushing commits...", object : Task<Unit>() {
            override fun call() {
                if (force) Git.pushForce(repository)
                else Git.push(repository)
            }

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() {
                when (exception) {
                    is PushRejectedException -> errorAlert(window, "Cannot Push to Remote Branch",
                            "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun createBranch(repository: LocalRepository) {
        textInputDialog(window, "Enter a New Branch Name", "Create", FontAwesome.codeFork()) { name ->
            State.startProcess("Branching...", object : Task<Unit>() {
                override fun call() = Git.branchCreate(repository, name)

                override fun succeeded() = State.fireRefresh(this)

                override fun failed() {
                    when (exception) {
                        is RefAlreadyExistsException -> errorAlert(window, "Cannot Create Branch",
                                "Branch '$name' does already exist in the working copy.")
                        is JGitInternalException -> errorAlert(window, "Cannot Create Branch",
                                "Invalid name '$name'.")
                        else -> exception.printStackTrace()
                    }
                }
            })
        }
    }

    private fun rebase(repository: LocalRepository) {
        val current = Git.head(repository)
        val branches = Git.branchListAll(repository).map { it.shortRef }.filter { it != current }
        choiceDialog(window, "Select a Branch for Rebasing", "Rebase", FontAwesome.levelUp().flipX(), branches) { branch ->
            State.startProcess("Rebasing...", object : Task<Unit>() {
                override fun call() = Git.rebase(repository, branch)

                override fun succeeded() = State.fireRefresh(this)

                override fun failed() = exception.printStackTrace()
            })
        }
    }

    private fun rebaseContinue(repository: LocalRepository) {
        State.startProcess("Rebasing...", object : Task<Unit>() {
            override fun call() = Git.rebaseContinue(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() {
                when (exception) {
                    is UnmergedPathsException ->
                        errorAlert(window, "Unresolved Conflicts",
                                "Cannot continue with rebase because there are unresolved conflicts.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun rebaseAbort(repository: LocalRepository) {
        State.startProcess("Aborting...", object : Task<Unit>() {
            override fun call() = Git.rebaseAbort(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun stash(repository: LocalRepository) {
        State.startProcess("Stashing files...", object : Task<Unit>() {
            override fun call() = Git.stash(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun stashPop(repository: LocalRepository) {
        State.startProcess("Applying stash...", object : Task<Unit>() {
            override fun call() = Git.stashPop(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() {
                when (exception) {
                    is StashApplyFailureException -> {
                        State.fireRefresh(this)
                        errorAlert(window, "Cannot Pop Stash",
                                "Applying stashed changes resulted in a conflict.\nTherefore the stash entry has been preserved.")
                    }
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun autoReset(repository: LocalRepository) {
        if (!confirmWarningAlert(window, "Auto Reset Branch", "Reset",
                "This will automatically reset the current branch to its remote branch.\nUnpushed commits will be lost.")) return

        State.startProcess("Resetting branch...", object : Task<Unit>() {
            override fun call() = Git.resetHard(repository)

            override fun succeeded() = State.fireRefresh(this)

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun autoSquash(repository: LocalRepository) {
        val commits = Git.logWithoutDefault(repository)
        val message = commits.joinToString("\n\n") { "# ${it.shortId}\n${it.fullMessage}" }
        val baseId = commits.last().parents.first()
        val count = commits.size
        textAreaDialog(window, "Auto Squash Branch", "Squash", FontAwesome.gavel(), message,
                "This will automatically squash all $count commits of the current branch.\n\nNew commit message:") {
            State.startProcess("Squashing branch...", object : Task<Unit>() {
                override fun call() = Git.rebaseSquash(repository, baseId, it)

                override fun succeeded() = State.fireRefresh(this)

                override fun failed() {
                    when (exception) {
                        is PrepareSquashException -> errorAlert(window, "Cannot Squash",
                                "${exception.message}\n\nPlease commit or stash them before squashing.")
                        is SquashException -> errorAlert(window, "Cannot Squash", exception.message!!)
                        else -> exception.printStackTrace()
                    }
                }
            })
        }
    }

}
