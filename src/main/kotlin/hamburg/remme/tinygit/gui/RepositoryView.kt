package hamburg.remme.tinygit.gui

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.domain.service.RepositoryService
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchUnpushedException
import hamburg.remme.tinygit.git.CheckoutException
import hamburg.remme.tinygit.git.gitBranchDelete
import hamburg.remme.tinygit.git.gitBranchDeleteForce
import hamburg.remme.tinygit.git.gitBranchList
import hamburg.remme.tinygit.git.gitBranchMove
import hamburg.remme.tinygit.git.gitCheckout
import hamburg.remme.tinygit.git.gitCheckoutRemote
import hamburg.remme.tinygit.git.gitHead
import hamburg.remme.tinygit.git.gitStashList
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
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
import java.util.concurrent.Callable

// TODO: create expand/collapse all actions
class RepositoryView : TreeView<RepositoryView.RepositoryEntry>() {

    val actions: Array<ActionGroup> get() = arrayOf(ActionGroup(settings))
    private val settings = Action("Settings", { Icons.cog() }, if (PlatformUtil.isMac()) "Shortcut+Comma" else null,
            disable = State.canSettings.not(),
            handler = { SettingsDialog(RepositoryService.activeRepository.get()!!, window).show() })

    private val window: Window get() = scene.window
    private val selectedEntry: RepositoryEntry? get() = selectionModel.selectedItem?.value
    private val cache: MutableMap<Repository, String> = mutableMapOf()

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it -> RepositoryService.activeRepository.set(it?.value?.repository) }

        // TODO: should be menu bar actions as well
        val canCheckout = Bindings.createBooleanBinding(
                Callable { selectedEntry.isBranch() },
                selectionModel.selectedItemProperty())
        val canRenameBranch = Bindings.createBooleanBinding(
                Callable { selectedEntry.isLocal() },
                selectionModel.selectedItemProperty())
        val canDeleteBranch = Bindings.createBooleanBinding(
                Callable { selectedEntry.isLocal() && !selectedEntry.isHead() },
                selectionModel.selectedItemProperty())
        val removeRepository = Action("Remove Repository (Del)", { Icons.trash() }, disable = State.canRemove.not(),
                handler = { removeRepository(selectedEntry!!) })
        val checkoutBranch = Action("Checkout Branch", { Icons.cloudDownload() }, disable = canCheckout.not(),
                handler = { checkout(selectedEntry!!) })
        val renameBranch = Action("Rename Branch", { Icons.pencil() }, disable = canRenameBranch.not(),
                handler = { renameBranch(selectedEntry!!) })
        val deleteBranch = Action("Delete Branch (Del)", { Icons.trash() }, disable = canDeleteBranch.not(),
                handler = { deleteBranch(selectedEntry!!) })

        contextMenu = contextMenu {
            isAutoHide = true
            +ActionGroup(removeRepository)
            +ActionGroup(checkoutBranch, renameBranch, deleteBranch)
            +ActionGroup(settings)
        }

        setOnKeyPressed {
            when (it.code) {
                KeyCode.SPACE -> it.consume()
                KeyCode.DELETE -> selectedEntry?.let {
                    if (it.isRoot()) removeRepository(it)
                    else if (it.isLocal() && !it.isHead()) deleteBranch(it)
                }
                else -> Unit
            }
        }
        setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                checkout(selectedEntry!!)
            }
        }

        RepositoryService.existingRepositories.addListener(ListChangeListener {
            while (it.next()) {
                when {
                    it.wasAdded() -> {
                        it.addedSubList.forEach { treeAdd(it) }
                        selectionModel.selectLast()
                    }
                    it.wasRemoved() -> it.removed.forEach { treeRemove(it) }
                }
            }
            if (it.list.isEmpty()) selectionModel.clearSelection() // forcefully clear selection
        })
        RepositoryService.existingRepositories.forEach { treeAdd(it) }
        State.addRefreshListener { treeUpdate(it) }

        Settings.setTree {
            root.children.flatMap { it.children + it }.map {
                Settings.TreeItem(it.value.repository.path, it.value.value, it.isExpanded)
            }
        }
        Settings.setTreeSelection {
            selectedEntry?.let { Settings.TreeItem(it.repository.path, it.value) } ?: Settings.TreeItem()
        }
        Settings.load { settings ->
            root.children.flatMap { it.children + it }
                    .filter { item -> settings.tree.any { it.repository == item.value.repository.path && it.name == item.value.value && it.expanded } }
                    .forEach { it.isExpanded = true }

            root.children.flatMap { it.children + it }.flatMap { it.children + it }
                    .find { it.value.repository.path == settings.treeSelection.repository && it.value.value == settings.treeSelection.name }
                    ?.let { selectionModel.select(it) }
                    ?: selectionModel.selectFirst()
            scrollTo(selectionModel.selectedIndex)
        }
    }

    private fun treeAdd(repository: Repository) {
        val localBranches = TreeItem(RepositoryEntry(repository, "Local Branches", EntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry(repository, "Remote Branches", EntryType.REMOTE))
        val stash = TreeItem(RepositoryEntry(repository, "Stash", EntryType.STASH))

        val repoTree = TreeItem(RepositoryEntry(repository, repository.shortPath, EntryType.REPOSITORY))
        repoTree.children.addAll(localBranches, remoteBranches, stash)
        root.children += repoTree

        treeUpdate(repository)
    }

    private fun treeUpdate(repository: Repository) {
        root.children.find { it.value.repository == repository }?.let {
            cache[repository] = gitHead(repository)
            val branchList = gitBranchList(repository)
            val stashList = gitStashList(repository)
            // TODO: selection might get lost on removed branches (e.g. after pruning)
            updateBranchItems(it.children[0].children, repository, branchList.filter { it.isLocal }, EntryType.LOCAL_BRANCH)
            updateBranchItems(it.children[1].children, repository, branchList.filter { it.isRemote }, EntryType.REMOTE_BRANCH)
            updateStashItems(it.children[2].children, repository, stashList)
        }
    }

    private fun treeRemove(repository: Repository) {
        root.children.find { it.value.repository == repository }?.let { root.children -= it }
    }

    private fun fireRefresh(repository: Repository) {
        treeUpdate(repository)
        State.fireRefresh()
    }

    private fun updateBranchItems(branchItems: ObservableList<TreeItem<RepositoryEntry>>,
                                  repository: Repository,
                                  branchList: List<Branch>,
                                  branchType: EntryType) {
        branchItems.addAll(branchList.filter { branch -> branchItems.none { it.value.value == branch.name } }
                .map { TreeItem(RepositoryEntry(repository, it.name, branchType)) })
        branchItems.removeAll(branchItems.filter { branch -> branchList.none { it.name == branch.value.value } })
        branchItems.sortWith(Comparator { left, right -> left.value.value.compareTo(right.value.value) })
    }

    private fun updateStashItems(stashItems: ObservableList<TreeItem<RepositoryEntry>>,
                                 repository: Repository,
                                 stashList: List<StashEntry>) {
        stashItems.addAll(stashList.filter { entry -> stashItems.none { it.value.value == entry.message } }
                .map { TreeItem(RepositoryEntry(repository, it.message, EntryType.STASH_ENTRY)) })
        stashItems.removeAll(stashItems.filter { entry -> stashList.none { it.message == entry.value.value } })
        stashItems.sortWith(Comparator { left, right -> stashList.indexOf(left.value.value) - stashList.indexOf(right.value.value) })
    }

    private fun List<StashEntry>.indexOf(message: String) = indexOfFirst { it.message == message }

    private fun removeRepository(entry: RepositoryEntry) {
        if (!confirmWarningAlert(window, "Remove Repository", "Remove",
                "Will remove the repository '${entry.repository}' from TinyGit, but keep it on the disk.")) return
        RepositoryService.remove(entry.repository)
    }

    private fun renameBranch(entry: RepositoryEntry) {
        textInputDialog(window, "Enter a New Branch Name", "Rename", Icons.pencil(), entry.value) { name ->
            try {
                gitBranchMove(entry.repository, entry.value, name)
                fireRefresh(entry.repository)
                root.children.flatMap { it.children + it }.flatMap { it.children + it }
                        .find { it.value.repository == entry.repository && it.value.value == name }
                        ?.let { selectionModel.select(it) }
            } catch (ex: BranchAlreadyExistsException) {
                errorAlert(window, "Cannot Create Branch",
                        "Branch '$name' does already exist in the working copy.")
            }
        }
    }

    private fun deleteBranch(entry: RepositoryEntry) {
        try {
            gitBranchDelete(entry.repository, entry.value)
            fireRefresh(entry.repository)
        } catch (ex: BranchUnpushedException) {
            if (confirmWarningAlert(window, "Delete Branch", "Delete",
                    "Branch '${entry.value}' was not deleted as it has unpushed commits.\n\nForce deletion?")) {
                gitBranchDeleteForce(entry.repository, entry.value)
                fireRefresh(entry.repository)
            }
        }
    }

    private fun checkout(entry: RepositoryEntry) {
        when (entry.type) {
            EntryType.LOCAL_BRANCH -> checkoutLocal(entry.repository, entry.value)
            EntryType.REMOTE_BRANCH -> checkoutRemote(entry.repository, entry.value)
            else -> Unit
        }
    }

    private fun checkoutLocal(repository: Repository, branch: String) {
        if (branch == gitHead(repository)) return

        State.startProcess("Switching branches...", object : Task<Unit>() {
            override fun call() = gitCheckout(repository, branch)

            override fun succeeded() = fireRefresh(repository)

            override fun failed() {
                when (exception) {
                    is CheckoutException -> errorAlert(window, "Cannot Switch Branches",
                            "There are local changes that would be overwritten by checkout.\nCommit or stash them.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun checkoutRemote(repository: Repository, branch: String) {
        State.startProcess("Getting remote branch...", object : Task<Unit>() {
            override fun call() = gitCheckoutRemote(repository, branch)

            override fun succeeded() = fireRefresh(repository)

            override fun failed() {
                when (exception) {
                    is CheckoutException -> checkoutLocal(repository, branch.substringAfter('/'))
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun RepositoryEntry?.isRoot() = this?.type == EntryType.REPOSITORY

    private fun RepositoryEntry?.isLocal() = this?.type == EntryType.LOCAL_BRANCH

    private fun RepositoryEntry?.isBranch(): Boolean {
        return !this.isHead() && this?.type == EntryType.LOCAL_BRANCH || this?.type == EntryType.REMOTE_BRANCH
    }

    private fun RepositoryEntry?.isHead() = this?.value == cache[this?.repository ?: ""]

    inner class RepositoryEntry(val repository: Repository, val value: String, val type: EntryType) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (repository != other.repository) return false
            if (value != other.value) return false
            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            var result = repository.hashCode()
            result = 31 * result + value.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

    }

    enum class EntryType {

        REPOSITORY,
        LOCAL, LOCAL_BRANCH,
        REMOTE, REMOTE_BRANCH,
        STASH, STASH_ENTRY

    }

    private inner class RepositoryEntryListCell : TreeCell<RepositoryEntry>() {

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when (item!!.type) {
                    EntryType.REPOSITORY -> repoItem(item)
                    EntryType.LOCAL -> item(Icons.hdd(), item.value)
                    EntryType.REMOTE -> item(Icons.cloud(), item.value)
                    EntryType.LOCAL_BRANCH -> branchItem(item)
                    EntryType.REMOTE_BRANCH -> item(Icons.codeFork(), item.value)
                    EntryType.STASH -> item(Icons.cubes(), item.value)
                    EntryType.STASH_ENTRY -> item(Icons.cube(), item.value)
                }.addClass("repository-cell")
            }
        }

        private fun item(icon: Node, value: String) = hbox {
            +icon
            +Label(value)
        }

        private fun repoItem(item: RepositoryEntry) = hbox {
            +Icons.database()
            +label {
                addStyle("-fx-font-weight:bold")
                text = item.value
            }
            +button {
                addClass("settings")
                graphic = Icons.cog()
                setOnAction { SettingsDialog(item.repository, window).show() }
            }
        }

        private fun branchItem(item: RepositoryEntry) = hbox {
            +Icons.codeFork()
            +Label(item.value)
            if (item.isHead()) {
                addClass("current")
                +Icons.check()
            }
        }

    }

}
