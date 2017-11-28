package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.FontAwesome
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.context
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.stage.Window
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import java.util.concurrent.Callable

class RepositoryView : TreeView<RepositoryView.RepositoryEntry>() {

    private val window: Window get() = scene.window
    private val headCache: MutableMap<String, String> = mutableMapOf()

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { State.selectedRepository = it.value.repository }
        }

        // TODO: should be menu bar actions as well
        val canModifyBranch = Bindings.createBooleanBinding(
                Callable { selectionModel.selectedItem?.value?.type == EntryType.LOCAL_BRANCH },
                selectionModel.selectedItemProperty())
        val removeRepository = Action("Remove Repository", handler = { removeRepository(selectionModel.selectedItem.value) })
        val renameBranch = Action("Rename Branch", disable = canModifyBranch.not(),
                handler = { renameBranch(selectionModel.selectedItem.value) })
        val removeBranch = Action("Remove Branch", disable = canModifyBranch.not(),
                handler = { removeBranch(selectionModel.selectedItem.value) })
        contextMenu = context {
            isAutoHide = true
            +ActionGroup(removeRepository)
            +ActionGroup(renameBranch, removeBranch)
        }

        setOnKeyPressed { if (it.code == KeyCode.SPACE) it.consume() }
        setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                val entry = selectionModel.selectedItem.value
                when (entry.type) {
                    EntryType.LOCAL_BRANCH -> checkout(entry.repository, entry.value)
                    EntryType.REMOTE_BRANCH -> checkoutRemote(entry.repository, entry.value)
                    else -> {
                        // do nothing
                    }
                }
            }
        }

        State.repositories.addListener(ListChangeListener {
            while (it.next()) {
                when {
                    it.wasAdded() -> {
                        it.addedSubList.forEach { addRepo(it) }
                        selectionModel.selectLast()
                    }
                    it.wasRemoved() -> it.removed.forEach { removeRepo(it) }
                }
            }
        })
        State.repositories.forEach { addRepo(it) }
        // TODO: this is being executed on startup
        // TODO: prob needs to refresh all repos or refresh on selection
        State.addRefreshListener { refreshRepo(it) }

        Settings.setTree {
            root.children.flatMap { it.children + it }.map {
                Settings.TreeItem(it.value.repository.path, it.value.value, it.isExpanded)
            }
        }
        Settings.setTreeSelection {
            val item = selectionModel.selectedItem
            Settings.TreeItem(item.value.repository.path, item.value.value)
        }
        Settings.load {
            val tree = tree
            root.children.flatMap { it.children + it }
                    .filter { item ->
                        tree.any { it.repository == item.value.repository.path && it.name == item.value.value && it.expanded }
                    }
                    .forEach { it.isExpanded = true }

            val selected = treeSelection
            root.children.flatMap { it.children + it }.flatMap { it.children + it }
                    .find { it.value.repository.path == selected.repository && it.value.value == selected.name }
                    ?.let { selectionModel.select(it) }
                    ?: selectionModel.selectFirst()
            scrollTo(selectionModel.selectedIndex)
        }
    }

    private fun addRepo(repository: LocalRepository) {
        val repoTree = TreeItem(RepositoryEntry(
                repository,
                repository.path.split("[\\\\/]".toRegex()).last(),
                EntryType.REPOSITORY))

        val localBranches = TreeItem(RepositoryEntry(repository, "Local Branches", EntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry(repository, "Remote Branches", EntryType.REMOTE))
        val tags = TreeItem(RepositoryEntry(repository, "Tags", EntryType.TAGS))
        val stash = TreeItem(RepositoryEntry(repository, "Stash", EntryType.STASH))

        repoTree.children.addAll(localBranches, remoteBranches, tags, stash)
        root.children += repoTree

        refreshRepo(repository)
    }

    private fun refreshRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let {
            headCache[repository.path] = Git.head(repository)
            val branchList = Git.branchListAll(repository)
            val stashList = Git.stashList(repository)
            // TODO: selection might get lost on removed branches (e.g. after pruning)
            updateBranchItems(it.children[0].children, repository, branchList.filter { it.local }, EntryType.LOCAL_BRANCH)
            updateBranchItems(it.children[1].children, repository, branchList.filter { it.remote }, EntryType.REMOTE_BRANCH)
            updateStashItems(it.children[3].children, repository, stashList)
        }
    }

    private fun removeRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let { root.children -= it }
    }

    private fun updateBranchItems(branchItems: ObservableList<TreeItem<RepositoryEntry>>,
                                  repository: LocalRepository,
                                  branchList: List<LocalBranch>,
                                  branchType: EntryType) {
        branchItems.addAll(branchList.filter { branch -> branchItems.none { it.value.value == branch.shortRef } }
                .map { TreeItem(RepositoryEntry(repository, it.shortRef, branchType)) })
        branchItems.removeAll(branchItems.filter { branch -> branchList.none { it.shortRef == branch.value.value } })
        branchItems.sortWith(Comparator { left, right -> left.value.value.compareTo(right.value.value) })
    }

    private fun updateStashItems(stashItems: ObservableList<TreeItem<RepositoryEntry>>,
                                 repository: LocalRepository,
                                 stashList: List<LocalStashEntry>) {
        stashItems.addAll(stashList.filter { entry -> stashItems.none { it.value.value == entry.message } }
                .map { TreeItem(RepositoryEntry(repository, it.message, EntryType.STASH_ENTRY)) })
        stashItems.removeAll(stashItems.filter { entry -> stashList.none { it.message == entry.value.value } })
        stashItems.sortWith(Comparator { left, right -> stashList.indexOf(left.value.value) - stashList.indexOf(right.value.value) })
    }

    private fun List<LocalStashEntry>.indexOf(message: String) = indexOfFirst { it.message == message }

    private fun removeRepository(entry: RepositoryEntry) {
        if (confirmWarningAlert(window,
                "Remove Repository",
                "Will remove the repository '${entry.repository}' from TinyGit, but keep it on the disk."))
            State.repositories -= entry.repository
    }

    private fun renameBranch(entry: RepositoryEntry) {
        if (entry.type != EntryType.LOCAL_BRANCH) {
            // TODO: proper item disabling
            contextMenu.hide()
            throw IllegalArgumentException("Can only be performed on branch entries.")
        }
        textInputDialog(window, FontAwesome.codeFork()) {
            Git.branchRename(entry.repository, entry.value, it)
        }
        // TODO: loses selected of renamed branch
        State.fireRefresh()
    }

    private fun removeBranch(entry: RepositoryEntry) {
        if (entry.type != EntryType.LOCAL_BRANCH) {
            // TODO: proper item disabling
            contextMenu.hide()
            throw IllegalArgumentException("Can only be performed on branch entries.")
        }
        Git.branchDelete(entry.repository, entry.value)
        State.fireRefresh()
    }

    private fun checkout(repository: LocalRepository, branch: String) {
        if (branch == Git.head(repository)) return

        State.addProcess("Switching branches...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.checkout(repository, branch)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is CheckoutConflictException -> errorAlert(window,
                            "Cannot Switch Branches",
                            "There are local changes that would be overwritten by checkout.\nCommit or stash them.")
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    private fun checkoutRemote(repository: LocalRepository, branch: String) {
        State.addProcess("Getting remote branch...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.checkoutRemote(repository, branch)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is RefAlreadyExistsException -> checkout(repository, branch.substringAfter('/'))
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = Platform.runLater { State.removeProcess() }
        })
    }

    inner class RepositoryEntry(val repository: LocalRepository, val value: String, val type: EntryType) {

        val isHead: Boolean get() = value == headCache[repository.path]

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (value != other.value) return false
            if (repository != other.repository) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + repository.hashCode()
            return result
        }

    }

    enum class EntryType {

        REPOSITORY,
        LOCAL, LOCAL_BRANCH,
        REMOTE, REMOTE_BRANCH,
        TAGS, TAG,
        STASH, STASH_ENTRY

    }

    private inner class RepositoryEntryListCell : TreeCell<RepositoryEntry>() {

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when (item!!.type) {
                    EntryType.REPOSITORY -> repoItem(item)
                    EntryType.LOCAL -> item(FontAwesome.desktop(), item.value)
                    EntryType.REMOTE -> item(FontAwesome.cloud(), item.value)
                    EntryType.LOCAL_BRANCH -> branchItem(item)
                    EntryType.REMOTE_BRANCH -> item(FontAwesome.codeFork(), item.value)
                    EntryType.TAGS -> item(FontAwesome.tags(), item.value)
                    EntryType.TAG -> item(FontAwesome.tag(), item.value)
                    EntryType.STASH -> item(FontAwesome.cubes(), item.value)
                    EntryType.STASH_ENTRY -> item(FontAwesome.cube(), item.value)
                }.addClass("repository-cell")
            }
        }

        private fun item(icon: Node, value: String) = hbox {
            +icon
            +Label(value)
        }

        private fun repoItem(item: RepositoryEntry) = hbox {
            +label {
                addStyle("-fx-font-weight:bold")
                text = item.value
            }
            +button {
                addClass("settings")
                graphic = FontAwesome.cog()
                setOnAction { SettingsDialog(item.repository, window).show() }
            }
        }

        private fun branchItem(item: RepositoryEntry) = hbox {
            +FontAwesome.codeFork()
            +Label(item.value)
            if (item.isHead) {
                addClass("current")
                +FontAwesome.check()
            }
        }

    }

}
