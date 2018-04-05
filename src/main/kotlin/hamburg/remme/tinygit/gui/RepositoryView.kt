package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.I18N
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
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.builder.textInputDialog
import hamburg.remme.tinygit.gui.builder.tree
import hamburg.remme.tinygit.gui.builder.vbox
import hamburg.remme.tinygit.gui.builder.vgrow
import hamburg.remme.tinygit.gui.component.Icons
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import hamburg.remme.tinygit.isMac
import hamburg.remme.tinygit.json
import hamburg.remme.tinygit.shortName
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

private const val DEFAULT_STYLE_CLASS = "repository-view"
private const val CONTENT_STYLE_CLASS = "${DEFAULT_STYLE_CLASS}__content"
private const val PATH_STYLE_CLASS = "path"
private const val REPO_VALUE_STYLE_CLASS = "repository-value-cell"
private const val REPO_LIST_STYLE_CLASS = "repository-list-cell"
private const val REPO_TREE_STYLE_CLASS = "repository-tree-cell"
private const val CURRENT_STYLE_CLASS = "current"
private const val DETACHED_STYLE_CLASS = "detached"

/**
 * Navigational tree view. Active [Repository] can be selected here and modified.
 * The tree view displays local branches, remote branches, tags and stashes.
 *
 * The view will also modify the global state depending on the selected [Repository].
 *
 * There are also shortcuts for executing different file actions:
 *  * `R`   - for renaming local branches
 *  * `Del` - for deleting branches and stashes
 * These actions can also be triggered with a context menu.
 *
 *
 * ```
 *   ┏━━━━━━━━━━━━━━━━━━━┯━━━━━━━━┓
 *   ┃ ToolBar           │ Button ┃
 *   ┠───────────────────┴────────┨
 *   ┃ > Local Branches           ┃
 *   ┃   > master                 ┃
 *   ┃ > Remote Branches          ┃
 *   ┃   > origin/master          ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┃                            ┃
 *   ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
 * ```
 *
 *
 * @todo: expand arrow are oftentimes buggy or completely broken
 */
class RepositoryView : VBoxBuilder() {

    private val repoService = TinyGit.repositoryService
    private val branchService = TinyGit.branchService
    private val tagService = TinyGit.tagService
    private val stashService = TinyGit.stashService
    private val window get() = scene.window
    private val tree: TreeView<Any>

    init {
        addClass(DEFAULT_STYLE_CLASS)

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

        val localBranches = RootTreeItem(Icons.hdd(), I18N["repository.localBranches"])
        val remoteBranches = RootTreeItem(Icons.cloud(), I18N["repository.remoteBranches"])
        val tags = RootTreeItem(Icons.tags(), I18N["repository.tags"])
        val stash = RootTreeItem(Icons.cubes(), I18N["repository.stash"])

        tree = tree {
            addClass(CONTENT_STYLE_CLASS)
            vgrow(Priority.ALWAYS)
            setCellFactory { RepositoryEntryTreeCell() }

            +localBranches
            +remoteBranches
            +tags
            +stash

            val renameKey = KeyCode.R
            val deleteKey = KeyCode.DELETE

            val canCheckout = Bindings.createBooleanBinding(Callable { selectedValue.isBranch() && !selectedValue.isHead() }, selectionModel.selectedItemProperty())
            val canRenameBranch = Bindings.createBooleanBinding(Callable { selectedValue.isLocal() }, selectionModel.selectedItemProperty())
            val canDeleteBranch = Bindings.createBooleanBinding(Callable { selectedValue.isBranch() && !selectedValue.isHead() }, selectionModel.selectedItemProperty())
            val canApplyStash = Bindings.createBooleanBinding(Callable { selectedValue.isStash() }, selectionModel.selectedItemProperty())
            val canDeleteStash = Bindings.createBooleanBinding(Callable { selectedValue.isStash() }, selectionModel.selectedItemProperty())
            val checkoutBranch = Action(I18N["repository.checkoutBranch"], { Icons.check() }, disabled = canCheckout.not(),
                    handler = { checkout(selectedValue as Branch) })
            val renameBranch = Action("${I18N["repository.renameBranch"]} (${renameKey.shortName})", { Icons.pencil() }, disabled = canRenameBranch.not(),
                    handler = { renameBranch(selectedValue as Branch) })
            val deleteBranch = Action("${I18N["repository.deleteBranch"]} (${deleteKey.shortName})", { Icons.trash() }, disabled = canDeleteBranch.not(),
                    handler = { deleteBranch(selectedValue as Branch) })
            val applyStash = Action(I18N["repository.applyStash"], { Icons.cube() }, disabled = canApplyStash.not(),
                    handler = { applyStash(selectedValue as StashEntry) })
            val deleteStash = Action("${I18N["repository.deleteStash"]} (${deleteKey.shortName})", { Icons.trash() }, disabled = canDeleteStash.not(),
                    handler = { deleteStash(selectedValue as StashEntry) })

            contextMenu = contextMenu {
                isAutoHide = true
                +ActionGroup(checkoutBranch, renameBranch, deleteBranch)
                +ActionGroup(applyStash, deleteStash)
            }
            setOnKeyPressed {
                if (!it.isShortcutDown) when (it.code) {
                    renameKey -> if (canRenameBranch.get()) renameBranch(selectedValue as Branch)
                    deleteKey -> {
                        if (canDeleteBranch.get()) deleteBranch(selectedValue as Branch)
                        if (canDeleteStash.get()) deleteStash(selectedValue as StashEntry)
                    }
                    else -> Unit
                }
            }
            setOnMouseClicked {
                if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                    if (canCheckout.get()) checkout(selectedValue as Branch)
                }
            }
        }
        +tree

        branchService.head.addListener { _ -> tree.refresh() }
        branchService.branches.addListener(ListChangeListener {
            localBranches.children.updateEntries(it.list.filter { it.isLocal })
            remoteBranches.children.updateEntries(it.list.filter { it.isRemote })
            ensureHeadSelection()
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

    private fun ensureHeadSelection() {
        if (tree.selectionModel.isEmpty || tree.selectionModel.selectedItem?.value !is Branch) {
            val headItem = tree.root.children.flatMap { it.children }
                    .filter { it.value is Branch }
                    .find { branchService.isHead(it.value as Branch) }
            tree.selectionModel.select(headItem)
        }
    }

    private fun renameBranch(branch: Branch) {
        textInputDialog(window, I18N["dialog.renameBranch.header"], I18N["dialog.renameBranch.button"], Icons.pencil(), branch.name) { name ->
            branchService.rename(
                    branch,
                    name,
                    { errorAlert(window, I18N["dialog.cannotRenameBranch.header"], I18N["dialog.cannotRenameBranch.text", name]) })
        }
    }

    private fun deleteBranch(branch: Branch) {
        if (branch.isLocal) deleteLocalBranch(branch) else deleteRemoteBranch(branch)
    }

    private fun deleteLocalBranch(branch: Branch) {
        branchService.deleteLocal(
                branch,
                false,
                {
                    if (confirmWarningAlert(window, I18N["dialog.cannotDeleteBranch.header"], I18N["dialog.cannotDeleteBranch.button"], I18N["dialog.cannotDeleteBranch.text", branch])) {
                        branchService.deleteLocal(branch, true)
                    }
                })
    }

    private fun deleteRemoteBranch(branch: Branch) {
        if (!confirmWarningAlert(window, I18N["dialog.deleteBranch.header"], I18N["dialog.deleteBranch.button"], I18N["dialog.deleteBranch.text", branch])) return
        branchService.deleteRemote(branch)
    }

    private fun checkout(branch: Branch) {
        if (branch.isLocal) checkoutLocal(branch) else checkoutRemote(branch)
    }

    private fun checkoutLocal(branch: Branch) {
        branchService.checkoutLocal(
                branch,
                { errorAlert(window, I18N["dialog.cannotSwitch.header"], I18N["dialog.cannotSwitch.text"]) })
    }

    private fun checkoutRemote(branch: Branch) {
        branchService.checkoutRemote(
                branch,
                { errorAlert(window, I18N["dialog.cannotSwitch.header"], I18N["dialog.cannotSwitch.text"]) })
    }

    private fun applyStash(stashEntry: StashEntry) {
        stashService.apply(stashEntry, { errorAlert(window, I18N["dialog.cannotApply.header"], I18N["dialog.cannotApply.text"]) })
    }

    private fun deleteStash(stashEntry: StashEntry) {
        if (!confirmWarningAlert(window, I18N["dialog.deleteStash.header"], I18N["dialog.deleteStash.button"], I18N["dialog.deleteStash.text", stashEntry])) return
        stashService.drop(stashEntry)
    }

    private fun Any?.isBranch() = this is Branch

    private fun Any?.isLocal() = this is Branch && isLocal

    private fun Any?.isHead() = this is Branch && branchService.isHead(this)

    private fun Any?.isStash() = this is StashEntry

    private class Root(val icon: Node, val text: String)

    private class RootTreeItem(icon: Node, text: String) : TreeItem<Any>(Root(icon, text))

    private class RepositoryValueCell : ListCell<Repository>() {

        init {
            addClass(REPO_VALUE_STYLE_CLASS)
        }

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            text = item?.shortPath
        }

    }

    private class RepositoryListCell : ListCell<Repository>() {

        private val name = label {}
        private val path = label { addClass(PATH_STYLE_CLASS) }

        init {
            addClass(REPO_LIST_STYLE_CLASS)
            graphic = vbox {
                +name
                +path
            }
        }

        override fun updateItem(item: Repository?, empty: Boolean) {
            super.updateItem(item, empty)
            name.text = item?.shortPath
            path.text = if (isMac) item?.path?.stripHome() else item?.path
        }

    }

    private inner class RepositoryEntryTreeCell : TreeCell<Any>() {

        init {
            addClass(REPO_TREE_STYLE_CLASS)
        }

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
                }
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
                addClass(DETACHED_STYLE_CLASS)
            } else if (branchService.isHead(branch)) {
                addClass(CURRENT_STYLE_CLASS)
            }
            if (branchService.isHead(branch)) +Icons.check()
        }

    }

}
