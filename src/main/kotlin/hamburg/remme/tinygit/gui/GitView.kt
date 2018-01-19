package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.service.BranchService
import hamburg.remme.tinygit.domain.service.DivergenceService
import hamburg.remme.tinygit.domain.service.MergeService
import hamburg.remme.tinygit.domain.service.RebaseService
import hamburg.remme.tinygit.domain.service.RemoteService
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.domain.service.StashService
import hamburg.remme.tinygit.git.gitLogExclusive
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionCollection
import hamburg.remme.tinygit.gui.builder.ActionGroup
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
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.AboutDialog
import hamburg.remme.tinygit.gui.dialog.CloneDialog
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text

class GitView : VBoxBuilder() {

    private val window get() = scene.window

    init {
        addClass("git-view")

        val repositoryView = RepositoryView()
        val commitLog = CommitLogView()
        val workingCopy = WorkingCopyView()
        val stats = StatsView()
        val tabs = TabPane(commitLog, workingCopy, stats)

        // File
        val cloneRepo = Action("Clone Repository", { Icons.clone() }, "Shortcut+Shift+O",
                handler = { CloneDialog(window).show() })
        val newRepo = Action("New Repository", { Icons.folder() }, "Shortcut+N",
                handler = { newRepo() })
        val addRepo = Action("Add Repository", { Icons.folderOpen() }, "Shortcut+O",
                handler = { addRepo() })
        val quit = Action("Quit TinyGit", { Icons.signOut() },
                handler = { Platform.exit() })
        // View
        val showCommits = Action("Show Commits", { Icons.list() }, "F1", RepositoryService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action("Show Working Copy", { Icons.hdd() }, "F2", RepositoryService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(workingCopy) })
        val showStats = Action("Show Statistics", { Icons.chartPie() }, "F3", RepositoryService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(stats) })
        // TODO: add F5 refresh
        // Repository
        val commit = Action("Commit", { Icons.plus() }, "Shortcut+K", State.canCommit.not(),
                { CommitDialog(window).show() })
        val push = Action("Push", { Icons.cloudUpload() }, "Shortcut+P", State.canPush.not(),
                { push(false) }, DivergenceService.ahead)
        val pushForce = Action("Force Push", { Icons.cloudUpload() }, "Shortcut+Shift+P", State.canForcePush.not(),
                { push(true) }, DivergenceService.ahead)
        val pull = Action("Pull", { Icons.cloudDownload() }, "Shortcut+L", State.canPull.not(),
                { pull() }, DivergenceService.behind)
        val fetch = Action("Fetch", { Icons.refresh() }, "Shortcut+F", State.canFetch.not(),
                { RemoteService.fetch() })
        val fetchGc = Action("GC", { Icons.eraser() }, "Shortcut+Shift+F", State.canGc.not(),
                { RepositoryService.gc() })
        val branch = Action("Branch", { Icons.codeFork() }, "Shortcut+B", State.canBranch.not(),
                { createBranch() })
        val merge = Action("Merge", { Icons.codeFork().flipY() }, "Shortcut+M", State.canMerge.not(),
                handler = { merge() })
        val mergeContinue = Action("Continue Merge", { Icons.forward() }, "Shortcut+Shift+M", State.canMergeContinue.not(),
                handler = { CommitDialog(window).show() })
        val mergeAbort = Action("Abort Merge", { Icons.timesCircle() }, disable = State.canMergeAbort.not(),
                handler = { MergeService.abort() })
        val rebase = Action("Rebase", { Icons.levelUp().flipX() }, "Shortcut+R", State.canRebase.not(),
                handler = { rebase() })
        val rebaseContinue = Action("Continue Rebase", { Icons.forward() }, "Shortcut+Shift+R", State.canRebaseContinue.not(),
                handler = { rebaseContinue() })
        val rebaseAbort = Action("Abort Rebase", { Icons.timesCircle() }, disable = State.canRebaseAbort.not(),
                handler = { RebaseService.abort() })
        val stash = Action("Stash", { Icons.cube() }, "Shortcut+S", State.canStash.not(),
                { StashService.stash() })
        val stashPop = Action("Pop Stash", { Icons.cube().flipXY() }, "Shortcut+Shift+S", State.canApplyStash.not(),
                { stashPop() })
        val reset = Action("Auto-Reset", { Icons.undo() }, disable = State.canReset.not(),
                handler = { autoReset() })
        val squash = Action("Auto-Squash", { Icons.gavel() }, disable = State.canSquash.not(),
                handler = { autoSquash() }, count = DivergenceService.aheadDefault)
        // ?
        val github = Action("Star TinyGit on GitHub", { Icons.github() },
                handler = { TinyGit.show("https://github.com/deso88/TinyGit") })
        val about = Action("About", { Icons.questionCircle() },
                handler = { AboutDialog(window).show() })
        val cmd = Action("Git Command", { Icons.terminal() }, disable = ReadOnlyBooleanWrapper(true),
                handler = { /* TODO */ })

        +menuBar {
            isUseSystemMenuBar = true
            +ActionCollection("File", ActionGroup(cloneRepo, newRepo, addRepo), ActionGroup(quit))
            +ActionCollection("View", ActionGroup(showCommits, showWorkingCopy, showStats))
            +ActionCollection("Repository",
                    ActionGroup(push, pushForce, pull, fetch, fetchGc),
                    ActionGroup(branch, merge, mergeContinue, mergeAbort),
                    ActionGroup(rebase, rebaseContinue, rebaseAbort),
                    ActionGroup(reset, squash),
                    *repositoryView.actions)
            +ActionCollection("Actions",
                    ActionGroup(commit),
                    *workingCopy.actions,
                    ActionGroup(stash, stashPop))
            +ActionCollection("?", ActionGroup(github, about))
        }
        +toolBar {
            visibleWhen(State.showToolBar)
            managedWhen(State.showToolBar)
            +ActionGroup(addRepo)
            +ActionGroup(commit, push, pull, fetch)
            +ActionGroup(branch, merge)
            +ActionGroup(stash, stashPop)
            +ActionGroup(reset, squash)
            addSpacer()
            +cmd
        }
        +toolBar {
            visibleWhen(State.showMergeBar)
            managedWhen(State.showMergeBar)
            +ActionGroup(addRepo)
            +ActionGroup(mergeContinue, mergeAbort)
            addSpacer()
            +cmd
        }
        +toolBar {
            visibleWhen(State.showRebaseBar)
            managedWhen(State.showRebaseBar)
            +ActionGroup(addRepo)
            +ActionGroup(rebaseContinue, rebaseAbort)
            addSpacer()
            +cmd
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
                    +Icons.folderOpen()
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

        Settings.setTabSelection { tabs.selectionModel.selectedIndex }
        Settings.load { tabs.selectionModel.select(it.tabSelection) }
    }

    private fun newRepo() {
        directoryChooser(window, "New Repository") { RepositoryService.init(it.absolutePath) }
    }

    private fun addRepo() {
        directoryChooser(window, "Add Repository") {
            RepositoryService.open(
                    it.absolutePath,
                    { errorAlert(window, "Invalid Repository", "'${it.absolutePath}' does not contain a valid '.git' directory.") })
        }
    }

    private fun pull() {
        RemoteService.pull(
                { errorAlert(window, "Cannot Pull From Remote Branch", it) },
                { errorAlert(window, "Connection Timed Out", "Please check the repository settings.\nCredentials or proxy settings may have changed.") })
    }

    private fun push(force: Boolean) {
        if (!RepositoryService.hasRemote.get()) {
            errorAlert(window, "Push", "No remote configured.")
            SettingsDialog(RepositoryService.activeRepository.get()!!, window).show()
            return
        } else if (force && !confirmWarningAlert(window, "Force Push", "Push",
                "This will rewrite the remote branch's history.\nChanges by others will be lost.")) {
            return
        }
        RemoteService.push(
                force,
                { errorAlert(window, "Cannot Push to Remote Branch", "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.") },
                { errorAlert(window, "Connection Timed Out", "Please check the repository settings.\nCredentials or proxy settings may have changed.") })
    }

    private fun createBranch() {
        textInputDialog(window, "Enter a New Branch Name", "Create", Icons.codeFork()) {
            BranchService.branch(
                    it,
                    { errorAlert(window, "Cannot Create Branch", "Branch '$it' does already exist in the working copy.") },
                    { errorAlert(window, "Cannot Create Branch", "Invalid name '$it'.") })
        }
    }

    private fun merge() {
        val current = BranchService.head.get()
        val branches = BranchService.branches.map { it.name }.filter { it != current }
        choiceDialog(window, "Select a Branch to Merge", "Merge", Icons.codeFork().flipY(), branches) {
            MergeService.merge(it)
        }
    }

    private fun rebase() {
        val current = BranchService.head.get()
        val branches = BranchService.branches.map { it.name }.filter { it != current }
        choiceDialog(window, "Select a Branch for Rebasing", "Rebase", Icons.levelUp().flipX(), branches) {
            RebaseService.rebase(it)
        }
    }

    private fun rebaseContinue() {
        RebaseService.`continue`({
            errorAlert(window, "Unresolved Conflicts",
                    "Cannot continue with rebase because there are unresolved conflicts.")
        })
    }

    private fun stashPop() {
        StashService.pop({
            errorAlert(window, "Cannot Pop Stash",
                    "Applying stashed changes resulted in a conflict.\nTherefore the stash entry has been preserved.")
        })
    }

    private fun autoReset() {
        if (!confirmWarningAlert(window, "Auto Reset Branch", "Reset",
                "This will automatically reset the current branch to its remote branch.\nUnpushed commits will be lost.")) return
        BranchService.autoReset()
    }

    private fun autoSquash() {
        val commits = gitLogExclusive(RepositoryService.activeRepository.get()!!)
        val message = commits.joinToString("\n") { "# ${it.shortId}\n${it.fullMessage}" } // TODO: too many newlines
        val baseId = commits.last().parents[0]
        val count = commits.size
        textAreaDialog(window, "Auto Squash Branch", "Squash", Icons.gavel(), message,
                "This will automatically squash all $count commits of the current branch.\n\nNew commit message:") {
            BranchService.autoSquash(baseId, it)
        }
    }

}
