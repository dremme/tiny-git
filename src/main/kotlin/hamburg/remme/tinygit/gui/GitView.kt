package hamburg.remme.tinygit.gui

import com.sun.javafx.PlatformUtil
import de.codecentric.centerdevice.MenuToolkit
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.git.defaultBranches
import hamburg.remme.tinygit.git.git
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
import hamburg.remme.tinygit.gui.builder.menu
import hamburg.remme.tinygit.gui.builder.menuBar
import hamburg.remme.tinygit.gui.builder.menuItem
import hamburg.remme.tinygit.gui.builder.progressIndicator
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
import hamburg.remme.tinygit.gui.dialog.CmdDialog
import hamburg.remme.tinygit.gui.dialog.CmdResultDialog
import hamburg.remme.tinygit.gui.dialog.CommitDialog
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.control.TabPane
import javafx.scene.layout.Priority
import javafx.scene.text.Text
import javafx.stage.Stage

class GitView : VBoxBuilder() {

    private val repoService = TinyGit.repositoryService
    private val branchService = TinyGit.branchService
    private val divergenceService = TinyGit.divergenceService
    private val mergeService = TinyGit.mergeService
    private val rebaseService = TinyGit.rebaseService
    private val remoteService = TinyGit.remoteService
    private val stashService = TinyGit.stashService
    private val state = TinyGit.state
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
        val showCommits = Action("Show Commit Log", { Icons.list() }, "F1", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action("Show Working Copy", { Icons.hdd() }, "F2", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(workingCopy) })
        val showStats = Action("Show Statistics", { Icons.chartPie() }, "F3", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(stats) })
        val refresh = Action("Refresh", { Icons.refresh() }, "F5", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { TinyGit.fireEvent() })
        // Repository
        val commit = Action("Commit", { Icons.plus() }, "Shortcut+K", state.canCommit.not(),
                { CommitDialog(window).show() })
        val push = Action("Push", { Icons.cloudUpload() }, "Shortcut+P", state.canPush.not(),
                { push(false) }, divergenceService.ahead)
        val pushForce = Action("Force Push", { Icons.cloudUpload() }, "Shortcut+Shift+P", state.canForcePush.not(),
                { push(true) }, divergenceService.ahead)
        val pull = Action("Pull", { Icons.cloudDownload() }, "Shortcut+L", state.canPull.not(),
                { pull() }, divergenceService.behind)
        val fetch = Action("Fetch", { Icons.refresh() }, "Shortcut+F", state.canFetch.not(),
                { remoteService.fetch() })
        val fetchGc = Action("GC", { Icons.eraser() }, "Shortcut+Shift+F", state.canGc.not(),
                { repoService.gc() })
        val branch = Action("Branch", { Icons.codeFork() }, "Shortcut+B", state.canBranch.not(),
                { createBranch() })
        val merge = Action("Merge", { Icons.codeFork().flipY() }, if (PlatformUtil.isMac()) "Shortcut+Shift+M" else "Shortcut+M", state.canMerge.not(),
                handler = { merge() })
        val mergeContinue = Action("Continue Merge", { Icons.forward() }, disable = state.canMergeContinue.not(),
                handler = { CommitDialog(window).show() })
        val mergeAbort = Action("Abort Merge", { Icons.timesCircle() }, disable = state.canMergeAbort.not(),
                handler = { mergeService.abort() })
        val rebase = Action("Rebase", { Icons.levelUp().flipX() }, "Shortcut+R", state.canRebase.not(),
                handler = { rebase() })
        val rebaseContinue = Action("Continue Rebase", { Icons.forward() }, disable = state.canRebaseContinue.not(),
                handler = { rebaseContinue() })
        val rebaseAbort = Action("Abort Rebase", { Icons.timesCircle() }, disable = state.canRebaseAbort.not(),
                handler = { rebaseService.abort() })
        val stash = Action("Stash", { Icons.cube() }, "Shortcut+S", state.canStash.not(),
                { stashService.create() })
        val stashPop = Action("Pop Stash", { Icons.cube().flipXY() }, "Shortcut+Shift+S", state.canApplyStash.not(),
                { stashPop() })
        val reset = Action("Auto-Reset", { Icons.undo() }, disable = state.canReset.not(),
                handler = { autoReset() })
        val squash = Action("Auto-Squash", { Icons.gavel() }, disable = state.canSquash.not(),
                handler = { autoSquash() }, count = divergenceService.aheadDefault)
        val settings = Action("Settings", { Icons.cog() }, disable = state.canSettings.not(),
                handler = { SettingsDialog(window).show() })
        val preferences = Action("Preferences", shortcut = "Shortcut+Comma", disable = state.canSettings.not(),
                handler = { SettingsDialog(window).show() })
        val removeRepo = Action("Remove Repository", { Icons.trash() }, disable = state.canRemove.not(),
                handler = { removeRepo() })
        // ?
        val github = Action("Star TinyGit on GitHub", { Icons.github() },
                handler = { TinyGit.showDocument("https://github.com/dremme/tiny-git") })
        val about = Action("About", { Icons.questionCircle() },
                handler = { AboutDialog(window).show() })
        val cmd = Action("Git Command", { Icons.terminal() }, disable = state.canCmd.not(),
                handler = { gitCommand() })

        if (PlatformUtil.isMac()) {
            val toolkit = MenuToolkit.toolkit()
            toolkit.setApplicationMenu(menu {
                text = "TinyGit"
                +menuItem(preferences)
                +SeparatorMenuItem()
                +toolkit.createHideMenuItem("TinyGit")
                +toolkit.createHideOthersMenuItem()
                +toolkit.createUnhideAllMenuItem()
                +SeparatorMenuItem()
                +toolkit.createQuitMenuItem("TinyGit")
            })
        }
        +menuBar {
            isUseSystemMenuBar = true
            val file = mutableListOf(ActionGroup(cloneRepo, newRepo, addRepo))
            val repository = mutableListOf(ActionGroup(push, pushForce, pull, fetch, fetchGc),
                    ActionGroup(branch, merge, mergeContinue, mergeAbort),
                    ActionGroup(rebase, rebaseContinue, rebaseAbort),
                    ActionGroup(reset, squash),
                    ActionGroup(removeRepo))
            if (!PlatformUtil.isMac()) {
                file += ActionGroup(quit)
                repository += ActionGroup(settings)
            }
            +ActionCollection("File", *file.toTypedArray())
            +ActionCollection("View",
                    ActionGroup(showCommits, showWorkingCopy, showStats),
                    ActionGroup(refresh))
            +ActionCollection("Repository", *repository.toTypedArray())
            +ActionCollection("Actions",
                    ActionGroup(commit),
                    *workingCopy.actions,
                    ActionGroup(stash, stashPop),
                    ActionGroup(cmd))
            if (PlatformUtil.isMac()) {
                val toolkit = MenuToolkit.toolkit()
                +menu {
                    text = "Window"
                    +toolkit.createMinimizeMenuItem()
                    +toolkit.createZoomMenuItem()
                    +menuItem {
                        shortcut = "Shortcut+Ctrl+F"
                        text = "Enter Fullscreen"
                        setOnAction {
                            val stage = window as Stage
                            if (stage.isFullScreen) {
                                text = "Enter Fullscreen"
                                stage.isFullScreen = false
                            } else {
                                text = "Exit Fullscreen"
                                stage.isFullScreen = true
                            }
                        }
                    }
                }
            }
            +ActionCollection("?", ActionGroup(github, about))
        }
        +toolBar {
            visibleWhen(state.showToolBar)
            managedWhen(state.showToolBar)
            +ActionGroup(addRepo)
            +ActionGroup(commit, push, pull, fetch)
            +ActionGroup(branch, merge)
            +ActionGroup(stash, stashPop)
            +ActionGroup(reset, squash)
            addSpacer()
            +cmd
        }
        +toolBar {
            visibleWhen(state.showMergeBar)
            managedWhen(state.showMergeBar)
            +ActionGroup(addRepo)
            +ActionGroup(mergeContinue, mergeAbort)
            addSpacer()
            +cmd
        }
        +toolBar {
            visibleWhen(state.showRebaseBar)
            managedWhen(state.showRebaseBar)
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
                visibleWhen(state.showGlobalInfo)
                +hbox {
                    addClass("box")
                    +Text("Click ")
                    +Icons.folderOpen()
                    +Text(" to add a repository.")
                }
            }
            +stackPane {
                addClass("progress-overlay")
                visibleWhen(state.showGlobalOverlay)
                +progressIndicator(48.0)
                +label { textProperty().bind(state.processText) }
            }
        }

        TinyGit.settings.addOnSave { it["tabSelection"] = tabs.selectionModel.selectedIndex }
        TinyGit.settings.load { tabs.selectionModel.select(it.getInt("tabSelection") ?: 0) }
    }

    private fun newRepo() {
        directoryChooser(window, "New Repository") { repoService.init(it.toString()) }
    }

    private fun addRepo() {
        directoryChooser(window, "Add Repository") {
            repoService.open(
                    it.toString(),
                    { errorAlert(window, "Invalid Repository", "'$it' does not contain a valid '.git' directory.") })
        }
    }

    private fun removeRepo() {
        val repository = repoService.activeRepository.get()!!
        if (!confirmWarningAlert(window, "Remove Repository", "Remove",
                        "Will remove the repository '$repository' from TinyGit, but keep it on the disk.")) return
        repoService.remove(repository)
    }

    private fun pull() {
        remoteService.pull(
                { errorAlert(window, "Cannot Pull From Remote Branch", it) },
                { errorAlert(window, "Cannot Complete Pull", "Pulling changes resulted in a conflict.") },
                { errorAlert(window, "Connection Timed Out", "Please check the repository settings.\nCredentials or proxy settings may have changed.") })
    }

    private fun push(force: Boolean) {
        if (!repoService.hasRemote.get()) {
            errorAlert(window, "Push", "No remote configured.")
            SettingsDialog(window).show()
            return
        } else if (force && !confirmWarningAlert(window, "Force Push", "Push",
                        "This will rewrite the remote branch's history.\nChanges by others will be lost.")) {
            return
        }
        remoteService.push(
                force,
                { errorAlert(window, "Cannot Push to Remote Branch", "Updates were rejected because the tip of the current branch is behind.\nPull before pushing again or force push.") },
                { errorAlert(window, "Connection Timed Out", "Please check the repository settings.\nCredentials or proxy settings may have changed.") })
    }

    private fun createBranch() {
        textInputDialog(window, "Enter a New Branch Name", "Create", Icons.codeFork()) {
            branchService.branch(
                    it,
                    { errorAlert(window, "Cannot Create Branch", "Branch '$it' does already exist in the working copy.") },
                    { errorAlert(window, "Cannot Create Branch", "Invalid name '$it'.") })
        }
    }

    private fun merge() {
        val current = branchService.head.get()
        val branches = branchService.branches.filter { it != current }
        choiceDialog(window, "Select a Branch to Merge", "Merge", Icons.codeFork().flipY(), branches) {
            mergeService.merge(
                    it,
                    { errorAlert(window, "Cannot Merge", "The merge resulted in a conflict.") },
                    { errorAlert(window, "Cannot Merge", "There are local changes that would be overwritten by checkout.\nCommit or stash them.") })
        }
    }

    private fun rebase() {
        val current = branchService.head.get()
        val branches = branchService.branches.filter { it != current }.sortedWith(Comparator { a, b ->
            when {
                defaultBranches.contains(a.name) && defaultBranches.contains(b.name) -> a.compareTo(b)
                defaultBranches.contains(a.name) -> -1
                defaultBranches.contains(b.name) -> 1
                else -> a.compareTo(b)
            }
        })
        choiceDialog(window, "Select a Branch for Rebasing", "Rebase", Icons.levelUp().flipX(), branches) {
            rebaseService.rebase(it, { errorAlert(window, "Cannot Rebase", it) })
        }
    }

    private fun rebaseContinue() {
        rebaseService.`continue`({ errorAlert(window, "Unresolved Conflicts", "Cannot continue with rebase because there are unresolved conflicts.") })
    }

    private fun stashPop() {
        stashService.pop({ errorAlert(window, "Cannot Pop Stash", "Applying stashed changes resulted in a conflict.\nThe stash entry has been preserved.") })
    }

    private fun autoReset() {
        if (!confirmWarningAlert(window, "Auto Reset Branch", "Reset", "This will automatically reset the current branch to its remote branch.\nUnpushed commits will be lost.")) return
        branchService.autoReset()
    }

    private fun autoSquash() {
        val commits = gitLogExclusive(repoService.activeRepository.get()!!)
        val message = commits.joinToString("\n") { "# ${it.shortId}\n${it.fullMessage}" } // TODO: too many newlines
        val baseId = commits.last().parents[0].id
        val count = commits.size
        textAreaDialog(window, "Auto Squash Branch", "Squash", Icons.gavel(), message,
                "This will automatically squash all $count commits of the current branch.\n\nNew commit message:") {
            branchService.autoSquash(baseId, it)
        }
    }

    private fun gitCommand() {
        val repository = repoService.activeRepository.get()!!
        CmdDialog(repository, window).showAndWait()?.let {
            TinyGit.execute("git ${it.joinToString(" ")}...", object : Task<String>() {
                override fun call() = git(repository, *it)

                override fun succeeded() {
                    TinyGit.fireEvent()
                    if (value.isNotBlank()) CmdResultDialog(value, window).show()
                }

                override fun failed() = exception.printStackTrace()
            })
        }
    }

}
