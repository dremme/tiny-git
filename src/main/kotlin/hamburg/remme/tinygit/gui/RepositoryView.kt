package hamburg.remme.tinygit.gui

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.Icons
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.textInputDialog
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
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.api.errors.NotMergedException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import java.util.concurrent.Callable

class RepositoryView : TreeView<RepositoryView.RepositoryEntry>() {

    val actions: Array<ActionGroup> get() = arrayOf(ActionGroup(settings))
    private val settings = Action("Settings", { Icons.cog() }, if (PlatformUtil.isMac()) "Shortcut+Comma" else null,
            disable = State.canSettings.not(),
            handler = { SettingsDialog(State.getSelectedRepository(), window).show() })

    private val window: Window get() = scene.window
    private val selectedEntry: RepositoryEntry? get() = selectionModel.selectedItem?.value
    private val cache: MutableMap<LocalRepository, String> = mutableMapOf()

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it -> State.setSelectedRepository(it?.value?.repository) }

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

        State.getRepositories().addListener(ListChangeListener {
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
        State.getRepositories().forEach { treeAdd(it) }
        State.addRefreshListener(this) { State.getRepositories().forEach { treeUpdate(it) } }

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

    private fun treeAdd(repository: LocalRepository) {
        val localBranches = TreeItem(RepositoryEntry(repository, "Local Branches", EntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry(repository, "Remote Branches", EntryType.REMOTE))
        val tags = TreeItem(RepositoryEntry(repository, "Tags", EntryType.TAGS))
        val stash = TreeItem(RepositoryEntry(repository, "Stash", EntryType.STASH))

        val repoTree = TreeItem(RepositoryEntry(repository, repository.shortPath, EntryType.REPOSITORY))
        repoTree.children.addAll(localBranches, remoteBranches, tags, stash)
        root.children += repoTree

        treeUpdate(repository)
    }

    private fun treeUpdate(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let {
            cache[repository] = Git.head(repository)
            val branchList = Git.branchListAll(repository)
            val stashList = Git.stashList(repository)
            // TODO: selection might get lost on removed branches (e.g. after pruning)
            updateBranchItems(it.children[0].children, repository, branchList.filter { it.local }, EntryType.LOCAL_BRANCH)
            updateBranchItems(it.children[1].children, repository, branchList.filter { it.remote }, EntryType.REMOTE_BRANCH)
            updateStashItems(it.children[3].children, repository, stashList)
        }
    }

    private fun treeRemove(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let { root.children -= it }
    }

    private fun fireRefresh(repository: LocalRepository) {
        treeUpdate(repository)
        State.fireRefresh(this)
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
        if (confirmWarningAlert(window, "Remove Repository", "Remove",
                "Will remove the repository '${entry.repository}' from TinyGit, but keep it on the disk."))
            State.removeRepository(entry.repository)
    }

    private fun renameBranch(entry: RepositoryEntry) {
        textInputDialog(window, "Enter a New Branch Name", "Rename", Icons.pencil(), entry.value) { name ->
            Git.branchRename(entry.repository, entry.value, name)
            fireRefresh(entry.repository)
            root.children.flatMap { it.children + it }.flatMap { it.children + it }
                    .find { it.value.repository == entry.repository && it.value.value == name }
                    ?.let { selectionModel.select(it) }
        }
    }

    private fun deleteBranch(entry: RepositoryEntry) {
        try {
            Git.branchDelete(entry.repository, entry.value)
        } catch (ex: NotMergedException) {
            if (confirmWarningAlert(window, "Delete Branch", "Delete",
                    "Branch '${entry.value}' was not deleted as it has not been merged yet.\n\nForce deletion?"))
                Git.branchDeleteForce(entry.repository, entry.value)
        }
        fireRefresh(entry.repository)
    }

    private fun checkout(entry: RepositoryEntry) {
        when (entry.type) {
            EntryType.LOCAL_BRANCH -> checkoutLocal(entry.repository, entry.value)
            EntryType.REMOTE_BRANCH -> checkoutRemote(entry.repository, entry.value)
            else -> Unit
        }
    }

    private fun checkoutLocal(repository: LocalRepository, branch: String) {
        if (branch == Git.head(repository)) return

        State.startProcess("Switching branches...", object : Task<Unit>() {
            override fun call() = Git.checkout(repository, branch)

            override fun succeeded() = fireRefresh(repository)

            override fun failed() {
                when (exception) {
                    is CheckoutConflictException -> errorAlert(window, "Cannot Switch Branches",
                            "There are local changes that would be overwritten by checkout.\nCommit or stash them.")
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun checkoutRemote(repository: LocalRepository, branch: String) {
        State.startProcess("Getting remote branch...", object : Task<Unit>() {
            override fun call() = Git.checkoutRemote(repository, branch)

            override fun succeeded() = fireRefresh(repository)

            override fun failed() {
                when (exception) {
                    is RefAlreadyExistsException -> checkoutLocal(repository, branch.substringAfter('/'))
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    private fun RepositoryEntry?.isRoot() = this?.let { it.type == EntryType.REPOSITORY } == true

    private fun RepositoryEntry?.isLocal() = this?.let { it.type == EntryType.LOCAL_BRANCH } == true

    private fun RepositoryEntry?.isBranch(): Boolean {
        return this?.let { !it.isHead() && it.type == EntryType.LOCAL_BRANCH || it.type == EntryType.REMOTE_BRANCH } == true
    }

    private fun RepositoryEntry?.isHead() = this?.let { it.value == cache[it.repository] } == true

    inner class RepositoryEntry(val repository: LocalRepository, val value: String, val type: EntryType) {

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
        TAGS, TAG,
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
                    EntryType.TAGS -> item(Icons.tags(), item.value)
                    EntryType.TAG -> item(Icons.tag(), item.value)
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
