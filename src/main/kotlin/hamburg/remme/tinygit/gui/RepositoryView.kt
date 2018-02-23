package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.domain.Tag
import hamburg.remme.tinygit.git.BranchAlreadyExistsException
import hamburg.remme.tinygit.git.BranchUnpushedException
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
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.beans.binding.Bindings
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
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
    private val tagService = TinyGit.tagService
    private val stashService = TinyGit.stashService
    private val window get() = scene.window
    private val tree: TreeView<RepositoryEntry>
    private val selectedEntry @Suppress("UNNECESSARY_SAFE_CALL") get() = tree?.selectionModel?.selectedItem?.value
    private val branchComparator = { e1: TreeItem<RepositoryEntry>, e2: TreeItem<RepositoryEntry> ->
        val b1 = e1.value.value as Branch
        val b2 = e2.value.value as Branch
        b1.name.compareTo(b2.name)
    }
    private val tagComparator = { e1: TreeItem<RepositoryEntry>, e2: TreeItem<RepositoryEntry> ->
        val b1 = e1.value.value as Tag
        val b2 = e2.value.value as Tag
        b1.name.compareTo(b2.name)
    }
    private val stashComparator = { e1: TreeItem<RepositoryEntry>, e2: TreeItem<RepositoryEntry> ->
        val b1 = e1.value.value as StashEntry
        val b2 = e2.value.value as StashEntry
        b1.id.compareTo(b2.id)
    }

    init {
        addClass("repository-view")

        val repository = comboBox<Repository>(repoService.existingRepositories) {
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

        val localBranches = TreeItem(RepositoryEntry("Local Branches", EntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry("Remote Branches", EntryType.REMOTE))
        val tags = TreeItem(RepositoryEntry("Tags", EntryType.TAGS))
        val stash = TreeItem(RepositoryEntry("Stash", EntryType.STASH))

        tree = tree {
            vgrow(Priority.ALWAYS)
            setCellFactory { RepositoryEntryTreeCell() }

            +localBranches
            +remoteBranches
            +tags
            +stash

            val canCheckout = Bindings.createBooleanBinding(Callable { !selectedEntry.isHead() && selectedEntry.isBranch() }, selectionModel.selectedItemProperty())
            val canRenameBranch = Bindings.createBooleanBinding(Callable { selectedEntry.isLocal() }, selectionModel.selectedItemProperty())
            val canDeleteBranch = Bindings.createBooleanBinding(Callable { !selectedEntry.isHead() && selectedEntry.isLocal() }, selectionModel.selectedItemProperty())
            val checkoutBranch = Action("Checkout Branch", { Icons.cloudDownload() }, disable = canCheckout.not(),
                    handler = { checkout(selectedEntry!!.value as Branch) })
            val canApplyStash = Bindings.createBooleanBinding(Callable { selectedEntry.isStash() }, selectionModel.selectedItemProperty())
            val canDeleteStash = Bindings.createBooleanBinding(Callable { selectedEntry.isStash() }, selectionModel.selectedItemProperty())
            val renameBranch = Action("Rename Branch", { Icons.pencil() }, disable = canRenameBranch.not(),
                    handler = { renameBranch(selectedEntry!!.value as Branch) })
            val deleteBranch = Action("Delete Branch (Del)", { Icons.trash() }, disable = canDeleteBranch.not(),
                    handler = { deleteBranch(selectedEntry!!.value as Branch) })
            val applyStash = Action("Apply Stash", { Icons.cube() }, disable = canApplyStash.not(),
                    handler = { applyStash(selectedEntry!!.value as StashEntry) })
            val deleteStash = Action("Delete Stash (Del)", { Icons.trash() }, disable = canDeleteStash.not(),
                    handler = { deleteStash(selectedEntry!!.value as StashEntry) })

            contextMenu = contextMenu {
                isAutoHide = true
                +ActionGroup(checkoutBranch, renameBranch, deleteBranch)
                +ActionGroup(applyStash, deleteStash)
            }
            setOnKeyPressed {
                if (!it.isShortcutDown) when (it.code) {
                    KeyCode.SPACE -> it.consume()
                    KeyCode.DELETE -> {
                        if (canDeleteBranch.get()) deleteBranch(selectedEntry!!.value as Branch)
                        if (canDeleteStash.get()) deleteStash(selectedEntry!!.value as StashEntry)
                    }
                    else -> Unit
                }
            }
            setOnMouseClicked {
                if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                    if (canCheckout.get()) checkout(selectedEntry!!.value as Branch)
                }
            }
        }
        +tree

        branchService.branches.addListener(ListChangeListener {
            localBranches.children.updateEntries(it.list.filter { it.isLocal }.map { it.toLocalEntry() }, branchComparator)
            remoteBranches.children.updateEntries(it.list.filter { it.isRemote }.map { it.toRemoteEntry() }, branchComparator)
        })
        tagService.tags.addListener(ListChangeListener { tags.children.updateEntries(it.list.map { it.toEntry() }, tagComparator) })
        stashService.stashEntries.addListener(ListChangeListener { stash.children.updateEntries(it.list.map { it.toEntry() }, stashComparator) })

        TinyGit.settings.setTree { tree.root.children.map { Settings.TreeItem(it.value.text, it.isExpanded) } }
        TinyGit.settings.setRepositorySelection { repository.value }
        TinyGit.settings.load { settings ->
            settings.tree.filter { it.expanded }
                    .mapNotNull { item -> tree.root.children.find { it.value.text == item.text } }
                    .forEach { it.isExpanded = true }
            settings.repositorySelection?.let { repository.selectionModel.select(it) } ?: repository.selectionModel.selectFirst()
        }
    }

    private fun Branch.toLocalEntry() = RepositoryEntry(name, EntryType.LOCAL_BRANCH, this)

    private fun Branch.toRemoteEntry() = RepositoryEntry(name, EntryType.REMOTE_BRANCH, this)

    private fun Tag.toEntry() = RepositoryEntry(name, EntryType.TAG, this)

    private fun StashEntry.toEntry() = RepositoryEntry(message, EntryType.STASH_ENTRY, this)

    private fun ObservableList<TreeItem<RepositoryEntry>>.updateEntries(updatedList: List<RepositoryEntry>, comparator: (TreeItem<RepositoryEntry>, TreeItem<RepositoryEntry>) -> Int) {
        addSorted(updatedList.filter { entry -> none { it.value == entry } }.map { TreeItem(it) }, comparator)
        removeAll(filter { entry -> updatedList.none { it == entry.value } })
    }

    private fun renameBranch(branch: Branch) {
        textInputDialog(window, "Enter a New Branch Name", "Rename", Icons.pencil(), branch.name) { name ->
            try {
                branchService.rename(branch, name)
                tree.root.children.flatMap { it.children + it }.flatMap { it.children + it }
                        .find { it.value.value == name }
                        ?.let { tree.selectionModel.select(it) }
            } catch (ex: BranchAlreadyExistsException) {
                errorAlert(window, "Cannot Create Branch",
                        "Branch '$name' does already exist in the working copy.")
            }
        }
    }

    private fun deleteBranch(branch: Branch) {
        try {
            branchService.delete(branch, false)
        } catch (ex: BranchUnpushedException) {
            if (confirmWarningAlert(window, "Delete Branch", "Delete", "Branch '$branch' was not deleted as it has unpushed commits.\n\nForce deletion?")) {
                branchService.delete(branch, true)
            }
        }
    }

    private fun checkout(branch: Branch) {
        if (branch.isLocal) checkoutLocal(branch) else checkoutRemote(branch)
    }

    private fun checkoutLocal(branch: Branch) {
        branchService.checkoutLocal(
                branch,
                { errorAlert(window, "Cannot Switch Branches", "There are local changes that would be overwritten by checkout.\nCommit or stash them.") })
    }

    private fun checkoutRemote(branch: Branch) {
        branchService.checkoutRemote(
                branch,
                { errorAlert(window, "Cannot Switch Branches", "There are local changes that would be overwritten by checkout.\nCommit or stash them.") })
    }

    private fun applyStash(stashEntry: StashEntry) {
        stashService.apply(stashEntry, { errorAlert(window, "Cannot Apply Stash", "Applying stashed changes resulted in a conflict.") })
    }

    private fun deleteStash(stashEntry: StashEntry) {
        if (confirmWarningAlert(window, "Delete Stash Entry", "Delete", "Stash entry '$stashEntry' cannot be restored.")) {
            stashService.drop(stashEntry)
        }
    }

    private fun RepositoryEntry?.isLocal() = this?.type == EntryType.LOCAL_BRANCH

    private fun RepositoryEntry?.isBranch() = this?.type == EntryType.LOCAL_BRANCH || this?.type == EntryType.REMOTE_BRANCH

    private fun RepositoryEntry?.isHead() = (this?.value as? Branch)?.let { branchService.isHead(it) } == true

    private fun RepositoryEntry?.isStash() = this?.type == EntryType.STASH_ENTRY

    class RepositoryEntry(val text: String, val type: EntryType, val value: Any = Unit) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

    }

    enum class EntryType {

        LOCAL, LOCAL_BRANCH,
        REMOTE, REMOTE_BRANCH,
        TAGS, TAG,
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
//        private val ahead = SimpleIntegerProperty()
//        private val behind = SimpleIntegerProperty()
//        private var task: Task<*>? = null

        init {
            addClass("repository-list-cell")
            graphic = vbox {
                spacing = 2.0 // TODO: CSS?
                +hbox {
                    spacing = 6.0 // TODO: CSS?
                    +name
//                    +hbox {
//                        addClass("repository-divergence")
//                        +label {
//                            visibleWhen(ahead.greater0())
//                            managedWhen(visibleProperty())
//                            textProperty().bind(ahead.asString())
//                            +Icons.arrowUp()
//                        }
//                        +label {
//                            visibleWhen(behind.greater0())
//                            managedWhen(visibleProperty())
//                            textProperty().bind(behind.asString())
//                            +Icons.arrowDown()
//                        }
//                    }
                }
                +path
            }
        }

        // TODO: not updating if something got pushed or committed
        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            name.text = item?.shortPath
            path.text = item?.path
//            if (!empty) updateDivergence(item!!)
        }

        // TODO: maybe too heavy for a list cell
//        private fun updateDivergence(item: Repository) {
//            task?.cancel()
//            ahead.set(0)
//            behind.set(0)
//            task = object : Task<Divergence>() {
//                override fun call() = gitDivergence(item,ser)
//
//                override fun succeeded() {
//                    ahead.set(value.ahead)
//                    behind.set(value.behind)
//                }
//            }.also { TinyGit.execute(it) }
//        }

    }

    private inner class RepositoryEntryTreeCell : TreeCell<RepositoryEntry>() {

        init {
            branchService.head.addListener { _ -> updateItem(item, isEmpty) }
        }

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when (item!!.type) {
                    EntryType.LOCAL -> item(Icons.hdd(), item.text)
                    EntryType.LOCAL_BRANCH -> branchItem(item)
                    EntryType.REMOTE -> item(Icons.cloud(), item.text)
                    EntryType.REMOTE_BRANCH -> item(Icons.codeFork(), item.text)
                    EntryType.TAGS -> item(Icons.tags(), item.text)
                    EntryType.TAG -> item(Icons.tag(), item.text)
                    EntryType.STASH -> item(Icons.cubes(), item.text)
                    EntryType.STASH_ENTRY -> item(Icons.cube(), item.text)
                }.addClass("repository-cell")
            }
        }

        private fun item(icon: Node, text: String) = hbox {
            +icon
            +Label(text)
        }

        private fun branchItem(item: RepositoryEntry) = hbox {
            +if (item.text == "HEAD") Icons.locationArrow() else Icons.codeFork()
            +Label(item.text)
            if (item.text == "HEAD") {
                addClass("detached")
            } else if (item.isHead()) {
                addClass("current")
            }
            if (item.isHead()) +Icons.check()
        }

    }

}
