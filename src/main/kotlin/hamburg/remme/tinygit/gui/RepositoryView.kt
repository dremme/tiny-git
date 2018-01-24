package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchUnpushedException
import hamburg.remme.tinygit.git.gitDivergence
import hamburg.remme.tinygit.gui.builder.Action
import hamburg.remme.tinygit.gui.builder.ActionGroup
import hamburg.remme.tinygit.gui.builder.VBoxBuilder
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.comboBox
import hamburg.remme.tinygit.gui.builder.confirmWarningAlert
import hamburg.remme.tinygit.gui.builder.contextMenu
import hamburg.remme.tinygit.gui.builder.errorAlert
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.builder.tree
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.concurrent.Task
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.Priority
import javafx.stage.Window
import javafx.util.Callback
import java.util.concurrent.Callable

class RepositoryView : VBoxBuilder() {

    private val repoService = TinyGit.repositoryService
    private val branchService = TinyGit.branchService
    private val stashService = TinyGit.stashService
    private val window: Window get() = scene.window
    private val repository: ComboBox<Repository>
    private val tree: TreeView<RepositoryEntry>
    private val selectedEntry: RepositoryEntry?
        @Suppress("UNNECESSARY_SAFE_CALL") get() = tree?.selectionModel?.selectedItem?.value
    private val localBranches: TreeItem<RepositoryEntry>
    private val remoteBranches: TreeItem<RepositoryEntry>
    private val stash: TreeItem<RepositoryEntry>

    init {
        addClass("repository-view")

        repository = comboBox<Repository>(repoService.existingRepositories) {
            cellFactory = Callback { RepositoryListCell() }
            selectionModel.selectedItemProperty().addListener { _, _, it -> repoService.activeRepository.set(it) }
            prefWidth = Int.MAX_VALUE.toDouble()
        }
        +hbox {
            +repository
            +button {
                graphic = Icons.cog()
                setOnAction { SettingsDialog(window).show() }
            }
        }

        localBranches = TreeItem(RepositoryEntry("Local Branches", EntryType.LOCAL))
        remoteBranches = TreeItem(RepositoryEntry("Remote Branches", EntryType.REMOTE))
        stash = TreeItem(RepositoryEntry("Stash", EntryType.STASH))

        tree = tree {
            vgrow(Priority.ALWAYS)
            setCellFactory { RepositoryEntryTreeCell() }

            +localBranches
            +remoteBranches
            +stash

            val canCheckout = Bindings.createBooleanBinding(
                    Callable { selectedEntry.isBranch() },
                    selectionModel.selectedItemProperty())
            val canRenameBranch = Bindings.createBooleanBinding(
                    Callable { selectedEntry.isLocal() },
                    selectionModel.selectedItemProperty())
            val canDeleteBranch = Bindings.createBooleanBinding(
                    Callable { selectedEntry.isLocal() && !selectedEntry.isHead() },
                    selectionModel.selectedItemProperty())
            val checkoutBranch = Action("Checkout Branch", { Icons.cloudDownload() }, disable = canCheckout.not(),
                    handler = { checkout(selectedEntry!!) })
            val renameBranch = Action("Rename Branch", { Icons.pencil() }, disable = canRenameBranch.not(),
                    handler = { renameBranch(selectedEntry!!) })
            val deleteBranch = Action("Delete Branch (Del)", { Icons.trash() }, disable = canDeleteBranch.not(),
                    handler = { deleteBranch(selectedEntry!!) })

            contextMenu = contextMenu {
                isAutoHide = true
                +ActionGroup(checkoutBranch, renameBranch, deleteBranch)
            }
            setOnKeyPressed {
                when (it.code) {
                    KeyCode.SPACE -> it.consume()
                    KeyCode.DELETE -> selectedEntry?.let { if (it.isLocal() && !it.isHead()) deleteBranch(it) }
                    else -> Unit
                }
            }
            setOnMouseClicked {
                if (it.button == MouseButton.PRIMARY && it.clickCount == 2) checkout(selectedEntry!!)
            }
        }
        +tree

        branchService.branches.addListener(ListChangeListener {
            updateBranches(localBranches.children, it.list.filter { it.isLocal }, EntryType.LOCAL_BRANCH)
            updateBranches(remoteBranches.children, it.list.filter { it.isRemote }, EntryType.REMOTE_BRANCH)
        })
        stashService.stashEntries.addListener(ListChangeListener { updateStashes(it.list) })

        TinyGit.settings.setTree { tree.root.children.map { Settings.TreeItem(it.value.value, it.isExpanded) } }
        TinyGit.settings.setRepositorySelection { repository.value }
        TinyGit.settings.load { settings ->
            settings.tree.filter { it.expanded }
                    .mapNotNull { item -> tree.root.children.find { it.value.value == item.value } }
                    .forEach { it.isExpanded = true }
            settings.repositorySelection?.let { repository.selectionModel.select(it) } ?: repository.selectionModel.selectFirst()
        }
    }

    private fun updateBranches(branches: ObservableList<TreeItem<RepositoryEntry>>, updatedList: List<Branch>, type: EntryType) {
        branches.addAll(updatedList.filter { branch -> branches.none { it.value.value == branch.name } }
                .map { TreeItem(RepositoryEntry(it.name, type)) })
        branches.removeAll(branches.filter { branch -> updatedList.none { it.name == branch.value.value } })
        FXCollections.sort(branches, { left, right -> left.value.value.compareTo(right.value.value) })
    }

    private fun updateStashes(updatedList: List<StashEntry>) {
        stash.children.addAll(updatedList.filter { entry -> stash.children.none { it.value.value == entry.message } }
                .map { TreeItem(RepositoryEntry(it.message, EntryType.STASH_ENTRY)) })
        stash.children.removeAll(stash.children.filter { entry -> updatedList.none { it.message == entry.value.value } })
        FXCollections.sort(stash.children, { left, right -> updatedList.indexOf(left.value.value) - updatedList.indexOf(right.value.value) })
    }

    private fun List<StashEntry>.indexOf(message: String) = indexOfFirst { it.message == message }

    private fun renameBranch(entry: RepositoryEntry) {
        textInputDialog(window, "Enter a New Branch Name", "Rename", Icons.pencil(), entry.value) { name ->
            try {
                branchService.rename(entry.value, name)
                tree.root.children.flatMap { it.children + it }.flatMap { it.children + it }
                        .find { it.value.value == name }
                        ?.let { tree.selectionModel.select(it) }
            } catch (ex: BranchAlreadyExistsException) {
                errorAlert(window, "Cannot Create Branch",
                        "Branch '$name' does already exist in the working copy.")
            }
        }
    }

    private fun deleteBranch(entry: RepositoryEntry) {
        try {
            branchService.delete(entry.value, false)
        } catch (ex: BranchUnpushedException) {
            if (confirmWarningAlert(window, "Delete Branch", "Delete",
                    "Branch '${entry.value}' was not deleted as it has unpushed commits.\n\nForce deletion?")) {
                branchService.delete(entry.value, true)
            }
        }
    }

    private fun checkout(entry: RepositoryEntry) {
        when (entry.type) {
            EntryType.LOCAL_BRANCH -> checkoutLocal(entry.value)
            EntryType.REMOTE_BRANCH -> checkoutRemote(entry.value)
            else -> Unit
        }
    }

    private fun checkoutLocal(branch: String) {
        branchService.checkoutLocal(
                branch,
                { errorAlert(window, "Cannot Switch Branches", "There are local changes that would be overwritten by checkout.\nCommit or stash them.") })
    }

    private fun checkoutRemote(branch: String) {
        branchService.checkoutRemote(
                branch,
                { errorAlert(window, "Cannot Switch Branches", "There are local changes that would be overwritten by checkout.\nCommit or stash them.") })
    }

    private fun RepositoryEntry?.isLocal() = this?.type == EntryType.LOCAL_BRANCH

    private fun RepositoryEntry?.isBranch(): Boolean {
        return !this.isHead() && this?.type == EntryType.LOCAL_BRANCH || this?.type == EntryType.REMOTE_BRANCH
    }

    private fun RepositoryEntry?.isHead() = this?.value == branchService.head.get()

    inner class RepositoryEntry(val value: String, val type: EntryType) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (value != other.value) return false
            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + type.hashCode()
            return result
        }

    }

    enum class EntryType {

        LOCAL, LOCAL_BRANCH,
        REMOTE, REMOTE_BRANCH,
        STASH, STASH_ENTRY

    }

    private class RepositoryListCell : ListCell<Repository>() {

        private var task: Task<*>? = null

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortPath
            if (!empty) updateDivergence(item!!)
        }

        // TODO: maybe too heavy for a list cell
        private fun updateDivergence(item: Repository) {
            task?.cancel()
            task = object : Task<Divergence>() {
                override fun call() = gitDivergence(item)

                override fun succeeded() {
                    val divergence = mutableListOf<String>()
                    if (value.ahead > 0) divergence += "↑ ${value.ahead}"
                    if (value.behind > 0) divergence += "↓ ${value.behind}"
                    divergence.takeIf { it.isNotEmpty() }?.let {
                        text = "${item.shortPath} (${it.joinToString(" ")})"
                    }
                }
            }.also { TinyGit.execute(it) }
        }

    }

    private inner class RepositoryEntryTreeCell : TreeCell<RepositoryEntry>() {

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when (item!!.type) {
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
