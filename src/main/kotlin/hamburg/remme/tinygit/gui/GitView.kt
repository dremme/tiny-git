package hamburg.remme.tinygit.gui

import com.sun.javafx.PlatformUtil
import de.codecentric.centerdevice.MenuToolkit
import hamburg.remme.tinygit.I18N
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
        val cloneRepo = Action(I18N["menu.clone"], { Icons.clone() }, "Shortcut+Shift+O",
                handler = { CloneDialog(window).show() })
        val newRepo = Action(I18N["menu.new"], { Icons.folder() }, "Shortcut+N",
                handler = { newRepo() })
        val addRepo = Action(I18N["menu.add"], { Icons.folderOpen() }, "Shortcut+O",
                handler = { addRepo() })
        val quit = Action(I18N["menu.quit"], { Icons.signOut() },
                handler = { Platform.exit() })
        // View
        val showCommits = Action(I18N["menu.showLog"], { Icons.list() }, "F1", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(commitLog) })
        val showWorkingCopy = Action(I18N["menu.showCopy"], { Icons.hdd() }, "F2", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(workingCopy) })
        val showStats = Action(I18N["menu.showStats"], { Icons.chartPie() }, "F3", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { tabs.selectionModel.select(stats) })
        val refresh = Action(I18N["menu.refresh"], { Icons.refresh() }, "F5", repoService.activeRepository.isNull, // TODO: own prop?
                handler = { TinyGit.fireEvent() })
        // Repository
        val commit = Action(I18N["menu.commit"], { Icons.plus() }, "Shortcut+K", state.canCommit.not(),
                { CommitDialog(window).show() })
        val push = Action(I18N["menu.push"], { Icons.cloudUpload() }, "Shortcut+P", state.canPush.not(),
                { push(false) }, divergenceService.ahead)
        val pushForce = Action(I18N["menu.forcePush"], { Icons.cloudUpload() }, "Shortcut+Shift+P", state.canForcePush.not(),
                { push(true) }, divergenceService.ahead)
        val pull = Action(I18N["menu.pull"], { Icons.cloudDownload() }, "Shortcut+L", state.canPull.not(),
                { pull() }, divergenceService.behind)
        val fetch = Action(I18N["menu.fetch"], { Icons.refresh() }, "Shortcut+F", state.canFetch.not(),
                { remoteService.fetch() })
        val fetchGc = Action(I18N["menu.gc"], { Icons.eraser() }, "Shortcut+Shift+F", state.canGc.not(),
                { repoService.gc() })
        val branch = Action(I18N["menu.branch"], { Icons.codeFork() }, "Shortcut+B", state.canBranch.not(),
                { createBranch() })
        val merge = Action(I18N["menu.merge"], { Icons.codeFork().flipY() }, if (PlatformUtil.isMac()) "Shortcut+Shift+M" else "Shortcut+M", state.canMerge.not(),
                handler = { merge() })
        val mergeContinue = Action(I18N["menu.continueMerge"], { Icons.forward() }, disable = state.canMergeContinue.not(),
                handler = { CommitDialog(window).show() })
        val mergeAbort = Action(I18N["menu.abortMerge"], { Icons.timesCircle() }, disable = state.canMergeAbort.not(),
                handler = { mergeService.abort() })
        val rebase = Action(I18N["menu.rebase"], { Icons.levelUp().flipX() }, "Shortcut+R", state.canRebase.not(),
                handler = { rebase() })
        val rebaseContinue = Action(I18N["menu.continueRebase"], { Icons.forward() }, disable = state.canRebaseContinue.not(),
                handler = { rebaseContinue() })
        val rebaseAbort = Action(I18N["menu.abortRebase"], { Icons.timesCircle() }, disable = state.canRebaseAbort.not(),
                handler = { rebaseService.abort() })
        val stash = Action(I18N["menu.stash"], { Icons.cube() }, "Shortcut+S", state.canStash.not(),
                { stashService.create() })
        val stashPop = Action(I18N["menu.popStash"], { Icons.cube().flipXY() }, "Shortcut+Shift+S", state.canApplyStash.not(),
                { stashPop() })
        val reset = Action(I18N["menu.autoReset"], { Icons.undo() }, disable = state.canReset.not(),
                handler = { autoReset() })
        val squash = Action(I18N["menu.autoSquash"], { Icons.gavel() }, disable = state.canSquash.not(),
                handler = { autoSquash() }, count = divergenceService.aheadDefault)
        val settings = Action(I18N["menu.settings"], { Icons.cog() }, disable = state.canSettings.not(),
                handler = { SettingsDialog(window).show() })
        val preferences = Action(I18N["menu.preferences"], shortcut = "Shortcut+Comma", disable = state.canSettings.not(),
                handler = { SettingsDialog(window).show() })
        val removeRepo = Action(I18N["menu.remove"], { Icons.trash() }, disable = state.canRemove.not(),
                handler = { removeRepo() })
        // ?
        val github = Action(I18N["menu.star"], { Icons.github() },
                handler = { TinyGit.showDocument("https://github.com/dremme/tiny-git") })
        val about = Action(I18N["menu.about"], { Icons.questionCircle() },
                handler = { AboutDialog(window).show() })
        val cmd = Action(I18N["menu.command"], { Icons.terminal() }, disable = state.canCmd.not(),
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
            +ActionCollection(I18N["menuBar.file"], *file.toTypedArray())
            +ActionCollection(I18N["menuBar.view"],
                    ActionGroup(showCommits, showWorkingCopy, showStats),
                    ActionGroup(refresh))
            +ActionCollection(I18N["menuBar.repository"], *repository.toTypedArray())
            +ActionCollection(I18N["menuBar.actions"],
                    ActionGroup(commit),
                    *workingCopy.actions,
                    ActionGroup(stash, stashPop),
                    ActionGroup(cmd))
            if (PlatformUtil.isMac()) {
                val toolkit = MenuToolkit.toolkit()
                +menu {
                    text = I18N["menuBar.window"]
                    +toolkit.createMinimizeMenuItem()
                    +toolkit.createZoomMenuItem()
                    +menuItem {
                        shortcut = "Shortcut+Ctrl+F"
                        text = I18N["menu.enterFullscreen"]
                        setOnAction {
                            val stage = window as Stage
                            if (stage.isFullScreen) {
                                text = I18N["menu.enterFullscreen"]
                                stage.isFullScreen = false
                            } else {
                                text = I18N["menu.exitFullscreen"]
                                stage.isFullScreen = true
                            }
                        }
                    }
                }
            }
            +ActionCollection(I18N["menuBar.help"], ActionGroup(github, about))
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
                +I18N["gitView.addRepositoryInfo", Icons.folderOpen()].addClass("box")
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
        directoryChooser(window, I18N["dialog.newRepository.title"]) { repoService.init(it.toString()) }
    }

    private fun addRepo() {
        directoryChooser(window, I18N["dialog.addRepository.title"]) {
            repoService.open(
                    it.toString(),
                    // TODO: the path could be stripped with ~ for Macs
                    { errorAlert(window, I18N["dialog.invalidRepository.header"], I18N["dialog.invalidRepository.text", it]) })
        }
    }

    private fun removeRepo() {
        val repository = repoService.activeRepository.get()!!
        if (!confirmWarningAlert(window, I18N["dialog.remove.header"], I18N["dialog.remove.button"], I18N["dialog.remove.text", repository])) return
        repoService.remove(repository)
    }

    private fun pull() {
        remoteService.pull(
                { errorAlert(window, I18N["dialog.cannotPull.header"], it) },
                { errorAlert(window, I18N["dialog.pullConflict.header"], I18N["dialog.pullConflict.text"]) },
                { errorAlert(window, I18N["dialog.timeout.header"], I18N["dialog.timeout.text"]) })
    }

    private fun push(force: Boolean) {
        if (!repoService.hasRemote.get()) {
            errorAlert(window, I18N["dialog.push.header"], I18N["dialog.noRemote.text"])
            SettingsDialog(window).show()
            return
        } else if (force && !confirmWarningAlert(window, I18N["dialog.forcePush.header"], I18N["dialog.forcePush.button"], I18N["dialog.forcePush.text"])) {
            return
        }
        remoteService.push(
                force,
                { errorAlert(window, I18N["dialog.cannotPush.header"], I18N["dialog.cannotPush.text"]) },
                { errorAlert(window, I18N["dialog.timeout.header"], I18N["dialog.timeout.text"]) })
    }

    private fun createBranch() {
        textInputDialog(window, I18N["dialog.newBranch.header"], I18N["dialog.newBranch.button"], Icons.codeFork()) {
            branchService.branch(
                    it,
                    { errorAlert(window, I18N["dialog.cannotBranch.header"], I18N["dialog.cannotBranch.text", it]) },
                    { errorAlert(window, I18N["dialog.cannotBranch.header"], I18N["dialog.invalidBranchName.text", it]) })
        }
    }

    private fun merge() {
        val current = branchService.head.get()
        val branches = branchService.branches.filter { it != current }
        choiceDialog(window, I18N["dialog.merge.header"], I18N["dialog.merge.button"], Icons.codeFork().flipY(), branches) {
            mergeService.merge(
                    it,
                    { errorAlert(window, I18N["dialog.cannotMerge.header"], I18N["dialog.mergeConflict.text"]) },
                    { errorAlert(window, I18N["dialog.cannotMerge.header"], I18N["dialog.cannotMerge.text"]) })
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
        choiceDialog(window, I18N["dialog.rebase.header"], I18N["dialog.rebase.button"], Icons.levelUp().flipX(), branches) {
            rebaseService.rebase(it, { errorAlert(window, I18N["dialog.cannotRebase.header"], it) })
        }
    }

    private fun rebaseContinue() {
        rebaseService.doContinue({ errorAlert(window, I18N["dialog.rebaseContinue.header"], I18N["dialog.rebaseContinue.text"]) })
    }

    private fun stashPop() {
        stashService.pop({ errorAlert(window, I18N["dialog.popStash.header"], I18N["dialog.popStash.text"]) })
    }

    private fun autoReset() {
        if (!confirmWarningAlert(window, I18N["dialog.autoReset.header"], I18N["dialog.autoReset.button"], I18N["dialog.autoReset.text"])) return
        branchService.autoReset()
    }

    private fun autoSquash() {
        val commits = gitLogExclusive(repoService.activeRepository.get()!!)
        val message = commits.joinToString("\n") { "# ${it.shortId}\n${it.fullMessage}" } // TODO: too many newlines
        val baseId = commits.last().parents[0].id
        textAreaDialog(window, I18N["dialog.autoSquash.header"], I18N["dialog.autoSquash.button"], Icons.gavel(), message,
                "${I18N["dialog.autoSquash.text", commits.size.toString()]}:") {
            branchService.autoSquash(baseId, it)
        }
    }

    private fun gitCommand() {
        val repository = repoService.activeRepository.get()!!
        CmdDialog(repository, window).showAndWait()?.let {
            TinyGit.execute("git ${it.joinToString(" ")}", object : Task<String>() {
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
