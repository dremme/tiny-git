package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchUnpushedException
import hamburg.remme.tinygit.git.gitDivergence
import hamburg.remme.tinygit.greater0
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
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.managedWhen
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.builder.tree
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.builder.visibleWhen
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleIntegerProperty
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
import javafx.util.Callback
import java.util.concurrent.Callable

class RepositoryView : VBoxBuilder() {

    private val repoService = TinyGit.repositoryService
    private val branchService = TinyGit.branchService
    private val stashService = TinyGit.stashService
    private val window get() = scene.window
    private val repository: ComboBox<Repository>
    private val tree: TreeView<RepositoryEntry>
    private val selectedEntry @Suppress("UNNECESSARY_SAFE_CALL") get() = tree?.selectionModel?.selectedItem?.value
    private val localBranches: TreeItem<RepositoryEntry>
    private val remoteBranches: TreeItem<RepositoryEntry>
    private val stash: TreeItem<RepositoryEntry>
    private val branchComparator = { b1: TreeItem<RepositoryEntry>, b2: TreeItem<RepositoryEntry> -> b1.value.value.compareTo(b2.value.value) }
    private val stashComparator = { b1: TreeItem<RepositoryEntry>, b2: TreeItem<RepositoryEntry> -> b1.value.userData.compareTo(b2.value.userData) }

    init {
        addClass("repository-view")

        repository = comboBox<Repository>(repoService.existingRepositories) {
            buttonCell = RepositoryValueCell()
            cellFactory = Callback { RepositoryListCell() }
            selectionModel.selectedItemProperty().addListener { _, _, it -> repoService.activeRepository.set(it) }
            prefWidth = Int.MAX_VALUE.toDouble()
            items.addListener(ListChangeListener { while (it.next()) if (it.wasAdded()) selectionModel.selectLast() })
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

            val canCheckout = Bindings.createBooleanBinding(Callable { selectedEntry.isBranch() }, selectionModel.selectedItemProperty())
            val canRenameBranch = Bindings.createBooleanBinding(Callable { selectedEntry.isLocal() }, selectionModel.selectedItemProperty())
            val canDeleteBranch = Bindings.createBooleanBinding(Callable { selectedEntry.isLocal() && !selectedEntry.isHead() }, selectionModel.selectedItemProperty())
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
            localBranches.children.updateEntries(it.list.filter { it.isLocal }.map { it.toLocalEntry() }, branchComparator)
            remoteBranches.children.updateEntries(it.list.filter { it.isRemote }.map { it.toRemoteEntry() }, branchComparator)
        })
        stashService.stashEntries.addListener(ListChangeListener { stash.children.updateEntries(it.list.map { it.toEntry() }, stashComparator) })

        TinyGit.settings.setTree { tree.root.children.map { Settings.TreeItem(it.value.value, it.isExpanded) } }
        TinyGit.settings.setRepositorySelection { repository.value }
        TinyGit.settings.load { settings ->
            settings.tree.filter { it.expanded }
                    .mapNotNull { item -> tree.root.children.find { it.value.value == item.value } }
                    .forEach { it.isExpanded = true }
            settings.repositorySelection?.let { repository.selectionModel.select(it) } ?: repository.selectionModel.selectFirst()
        }
    }

    private fun Branch.toLocalEntry() = RepositoryEntry(name, EntryType.LOCAL_BRANCH, (name == branchService.head.get()).toString())

    private fun Branch.toRemoteEntry() = RepositoryEntry(name, EntryType.REMOTE_BRANCH, (name == branchService.head.get()).toString())

    private fun StashEntry.toEntry() = RepositoryEntry(message, EntryType.STASH_ENTRY, id)

    private fun ObservableList<TreeItem<RepositoryEntry>>.updateEntries(updatedList: List<RepositoryEntry>, comparator: (TreeItem<RepositoryEntry>, TreeItem<RepositoryEntry>) -> Int) {
        addSorted(updatedList.filter { entry -> none { it.value == entry } }.map { TreeItem(it) }, comparator)
        removeAll(filter { entry -> updatedList.none { it == entry.value } })
    }

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
            if (confirmWarningAlert(window, "Delete Branch", "Delete", "Branch '${entry.value}' was not deleted as it has unpushed commits.\n\nForce deletion?")) {
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

    private fun RepositoryEntry?.isBranch() = !this.isHead() && this?.type == EntryType.LOCAL_BRANCH || this?.type == EntryType.REMOTE_BRANCH

    private fun RepositoryEntry?.isHead() = this?.userData == "true"

    class RepositoryEntry(val value: String, val type: EntryType, val userData: String = "") {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (value != other.value) return false
            if (userData != other.userData) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + userData.hashCode()
            return result
        }

    }

    enum class EntryType {

        LOCAL, LOCAL_BRANCH,
        REMOTE, REMOTE_BRANCH,
        STASH, STASH_ENTRY

    }

    private class RepositoryValueCell : ListCell<Repository>() {

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortPath
        }

    }

    private class RepositoryListCell : ListCell<Repository>() {

        private val name = Label()
        private val path = Label().addClass("repository-path")
        private val ahead = SimpleIntegerProperty()
        private val behind = SimpleIntegerProperty()
        private var task: Task<*>? = null

        init {
            addClass("repository-list-cell")
            graphic = vbox {
                spacing = 2.0 // TODO: CSS?
                +hbox {
                    spacing = 6.0 // TODO: CSS?
                    +name
                    +hbox {
                        addClass("repository-divergence")
                        +label {
                            graphic = Icons.arrowUp()
                            visibleWhen(ahead.greater0())
                            managedWhen(visibleProperty())
                            textProperty().bind(ahead.asString())
                        }
                        +label {
                            graphic = Icons.arrowDown()
                            visibleWhen(behind.greater0())
                            managedWhen(visibleProperty())
                            textProperty().bind(behind.asString())
                        }
                    }
                }
                +path
            }
        }

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            name.text = item?.shortPath
            path.text = item?.path
            if (!empty) updateDivergence(item!!)
        }

        // TODO: maybe too heavy for a list cell
        private fun updateDivergence(item: Repository) {
            task?.cancel()
            task = object : Task<Divergence>() {
                override fun call() = gitDivergence(item)

                override fun succeeded() {
                    ahead.set(value.ahead)
                    behind.set(value.behind)
                }
            }.also { TinyGit.execute(it) }
        }

    }

    private class RepositoryEntryTreeCell : TreeCell<RepositoryEntry>() {

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

        private fun RepositoryEntry?.isHead() = this?.userData == "true" // TODO: duplicated

    }

}
