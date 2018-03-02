package hamburg.remme.tinygit.gui

import com.sun.javafx.PlatformUtil
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.addSorted
import hamburg.remme.tinygit.domain.Branch
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.StashEntry
import hamburg.remme.tinygit.domain.Tag
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
import hamburg.remme.tinygit.json
import hamburg.remme.tinygit.stripHome
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
    private val tree: TreeView<Any>
    private val treeSelection @Suppress("UNNECESSARY_SAFE_CALL") get() = tree?.selectionModel?.selectedItem?.value

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

        val localBranches = RootTreeItem(Icons.hdd(), "Local Branches")
        val remoteBranches = RootTreeItem(Icons.cloud(), "Remote Branches")
        val tags = RootTreeItem(Icons.tags(), "Tags")
        val stash = RootTreeItem(Icons.cubes(), "Stash")

        tree = tree {
            vgrow(Priority.ALWAYS)
            setCellFactory { RepositoryEntryTreeCell() }

            +localBranches
            +remoteBranches
            +tags
            +stash

            val canCheckout = Bindings.createBooleanBinding(Callable { treeSelection.isBranch() && !treeSelection.isHead() }, selectionModel.selectedItemProperty())
            val canRenameBranch = Bindings.createBooleanBinding(Callable { treeSelection.isLocal() }, selectionModel.selectedItemProperty())
            val canDeleteBranch = Bindings.createBooleanBinding(Callable { treeSelection.isLocal() && !treeSelection.isHead() }, selectionModel.selectedItemProperty())
            val canApplyStash = Bindings.createBooleanBinding(Callable { treeSelection.isStash() }, selectionModel.selectedItemProperty())
            val canDeleteStash = Bindings.createBooleanBinding(Callable { treeSelection.isStash() }, selectionModel.selectedItemProperty())
            val checkoutBranch = Action("Checkout Branch", { Icons.cloudDownload() }, disable = canCheckout.not(),
                    handler = { checkout(treeSelection as Branch) })
            val renameBranch = Action("Rename Branch (R)", { Icons.pencil() }, disable = canRenameBranch.not(),
                    handler = { renameBranch(treeSelection as Branch) })
            val deleteBranch = Action("Delete Branch (Del)", { Icons.trash() }, disable = canDeleteBranch.not(),
                    handler = { deleteBranch(treeSelection as Branch) })
            val applyStash = Action("Apply Stash", { Icons.cube() }, disable = canApplyStash.not(),
                    handler = { applyStash(treeSelection as StashEntry) })
            val deleteStash = Action("Delete Stash (Del)", { Icons.trash() }, disable = canDeleteStash.not(),
                    handler = { deleteStash(treeSelection as StashEntry) })

            contextMenu = contextMenu {
                isAutoHide = true
                +ActionGroup(checkoutBranch, renameBranch, deleteBranch)
                +ActionGroup(applyStash, deleteStash)
            }
            setOnKeyPressed {
                if (!it.isShortcutDown) when (it.code) {
                    KeyCode.R -> if (canRenameBranch.get()) renameBranch(treeSelection as Branch)
                    KeyCode.DELETE -> {
                        if (canDeleteBranch.get()) deleteBranch(treeSelection as Branch)
                        if (canDeleteStash.get()) deleteStash(treeSelection as StashEntry)
                    }
                    else -> Unit
                }
            }
            setOnMouseClicked {
                if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                    if (canCheckout.get()) checkout(treeSelection as Branch)
                }
            }
        }
        +tree

        branchService.head.addListener { _ -> tree.refresh() }
        branchService.branches.addListener(ListChangeListener {
            localBranches.children.updateEntries(it.list.filter { it.isLocal })
            remoteBranches.children.updateEntries(it.list.filter { it.isRemote })
        })
        tagService.tags.addListener(ListChangeListener { tags.children.updateEntries(it.list) })
        stashService.stashEntries.addListener(ListChangeListener { stash.children.updateEntries(it.list) })

        TinyGit.settings.addOnSave {
            it["repositorySelection"] = json { +("path" to repository.value.path) }
            it["tree"] = tree.root.children.map {
                json {
                    +("index" to tree.root.children.indexOf(it))
                    +("expanded" to it.isExpanded)
                }
            }
        }
        TinyGit.settings.load { settings ->
            settings["repositorySelection"]
                    ?.let { repository.selectionModel.select(Repository(it.getString("path")!!)) }
                    ?: repository.selectionModel.selectFirst()
            settings.getList("tree")
                    ?.filter { it.getBoolean("expanded")!! }
                    ?.forEach { tree.root.children[it.getInt("index")!!].isExpanded = true }
        }
    }

    private fun ObservableList<TreeItem<Any>>.updateEntries(updatedList: List<Any>) {
        addSorted(updatedList.filter { entry -> none { it.value == entry } }.map { TreeItem(it) },
                { e1: TreeItem<*>, e2: TreeItem<*> ->
                    @Suppress("UNCHECKED_CAST") val b1 = e1.value as Comparable<Any>
                    @Suppress("UNCHECKED_CAST") val b2 = e2.value as Comparable<Any>
                    b1.compareTo(b2)
                })
        removeAll(filter { entry -> updatedList.none { it == entry.value } })
    }

    private fun renameBranch(branch: Branch) {
        textInputDialog(window, "Enter a New Branch Name", "Rename", Icons.pencil(), branch.name) { name ->
            branchService.rename(
                    branch,
                    name,
                    { errorAlert(window, "Cannot Create Branch", "Branch '$name' does already exist in the working copy.") })
        }
    }

    private fun deleteBranch(branch: Branch) {
        branchService.delete(
                branch,
                false,
                {
                    if (confirmWarningAlert(window, "Delete Branch", "Delete", "Branch '$branch' was not deleted as it has unpushed commits.\n\nForce deletion?")) {
                        branchService.delete(branch, true)
                    }
                })
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

    private fun Any?.isBranch() = this is Branch

    private fun Any?.isLocal() = this is Branch && isLocal

    private fun Any?.isHead() = this is Branch && branchService.isHead(this)

    private fun Any?.isStash() = this is StashEntry

    private class Root(val icon: Node, val text: String)

    private class RootTreeItem(icon: Node, text: String) : TreeItem<Any>(Root(icon, text))

    private class RepositoryValueCell : ListCell<Repository>() {

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortPath
        }

    }

    private class RepositoryListCell : ListCell<Repository>() {

        private val name = Label()
        private val path = Label().addClass("repository-path")

        init {
            addClass("repository-list-cell")
            graphic = vbox {
                spacing = 2.0 // TODO: CSS?
                +hbox {
                    spacing = 6.0 // TODO: CSS?
                    +name
                }
                +path
            }
        }

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            name.text = item?.shortPath
            path.text = if (PlatformUtil.isMac()) item?.path?.stripHome() else item?.path
        }

    }

    private inner class RepositoryEntryTreeCell : TreeCell<Any>() {

        override fun updateItem(item: Any?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when {
                    item is Root -> item(item.icon, item.text)
                    item is Branch && item.isLocal -> branchItem(item)
                    item is Branch && item.isRemote -> item(Icons.codeFork(), item.name)
                    item is Tag -> item(Icons.tag(), item.name)
                    item is StashEntry -> item(Icons.cube(), item.message)
                    else -> throw RuntimeException()
                }.addClass("repository-cell")
            }
        }

        private fun item(icon: Node, text: String) = hbox {
            +icon
            +Label(text)
        }

        private fun branchItem(branch: Branch) = hbox {
            +if (branchService.isDetached(branch)) Icons.locationArrow() else Icons.codeFork()
            +Label(branch.name)
            if (branchService.isDetached(branch)) {
                addClass("detached")
            } else if (branchService.isHead(branch)) {
                addClass("current")
            }
            if (branchService.isHead(branch)) +Icons.check()
        }

    }

}
