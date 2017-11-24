package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.api.Git
import hamburg.remme.tinygit.gui.builder.addClass
import hamburg.remme.tinygit.gui.builder.addStyle
import hamburg.remme.tinygit.gui.builder.button
import hamburg.remme.tinygit.gui.builder.hbox
import hamburg.remme.tinygit.gui.builder.label
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.application.Platform
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
import org.eclipse.jgit.api.errors.CheckoutConflictException
import org.eclipse.jgit.api.errors.RefAlreadyExistsException

class RepositoryView : TreeView<RepositoryView.RepositoryEntry>() {

    private val headCache: MutableMap<String, String> = mutableMapOf()

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { State.selectedRepository = it.value.repository }
        }

        setOnKeyPressed { if (it.code == KeyCode.SPACE) it.consume() }
        setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                val entry = selectionModel.selectedItem.value
                when (entry.type) {
                    RepositoryEntryType.LOCAL_BRANCH -> checkout(entry.repository, entry.value)
                    RepositoryEntryType.REMOTE_BRANCH -> checkoutRemote(entry.repository, entry.value)
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
                RepositoryEntryType.REPOSITORY))

        val localBranches = TreeItem(RepositoryEntry(repository, "Local Branches", RepositoryEntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry(repository, "Remote Branches", RepositoryEntryType.REMOTE))
        val tags = TreeItem(RepositoryEntry(repository, "Tags", RepositoryEntryType.TAGS))
        val stash = TreeItem(RepositoryEntry(repository, "Stash", RepositoryEntryType.STASH))

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
            updateBranchItems(it.children[0].children, repository, branchList.filter { it.local }, RepositoryEntryType.LOCAL_BRANCH)
            updateBranchItems(it.children[1].children, repository, branchList.filter { it.remote }, RepositoryEntryType.REMOTE_BRANCH)
            updateStashItems(it.children[3].children, repository, stashList)
        }
    }

    private fun removeRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let { root.children -= it }
    }

    private fun updateBranchItems(branchItems: ObservableList<TreeItem<RepositoryEntry>>,
                                  repository: LocalRepository,
                                  branchList: List<LocalBranch>,
                                  branchType: RepositoryEntryType) {
        branchItems.addAll(branchList.filter { branch -> branchItems.none { it.value.value == branch.shortRef } }
                .map { TreeItem(RepositoryEntry(repository, it.shortRef, branchType)) })
        branchItems.removeAll(branchItems.filter { branch -> branchList.none { it.shortRef == branch.value.value } })
        branchItems.sortWith(Comparator { left, right -> left.value.value.compareTo(right.value.value) })
    }

    private fun updateStashItems(stashItems: ObservableList<TreeItem<RepositoryEntry>>,
                                 repository: LocalRepository,
                                 stashList: List<LocalStashEntry>) {
        stashItems.addAll(stashList.filter { entry -> stashItems.none { it.value.value == entry.message } }
                .map { TreeItem(RepositoryEntry(repository, it.message, RepositoryEntryType.STASH_ENTRY)) })
        stashItems.removeAll(stashItems.filter { entry -> stashList.none { it.message == entry.value.value } })
        stashItems.sortWith(Comparator { left, right -> stashList.indexOf(left.value.value) - stashList.indexOf(right.value.value) })
    }

    private fun List<LocalStashEntry>.indexOf(message: String) = indexOfFirst { it.message == message }

    private fun checkout(repository: LocalRepository, branch: String) {
        if (branch == Git.head(repository)) return

        State.addProcess("Switching branches...")
        State.execute(object : Task<Unit>() {
            override fun call() = Git.checkout(repository, branch)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is CheckoutConflictException -> errorAlert(scene.window,
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

    inner class RepositoryEntry(val repository: LocalRepository, val value: String, val type: RepositoryEntryType) {

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

    enum class RepositoryEntryType {

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
                    RepositoryEntryType.REPOSITORY -> repoItem(item)
                    RepositoryEntryType.LOCAL -> item(FontAwesome.desktop(), item.value)
                    RepositoryEntryType.REMOTE -> item(FontAwesome.cloud(), item.value)
                    RepositoryEntryType.LOCAL_BRANCH -> branchItem(item)
                    RepositoryEntryType.REMOTE_BRANCH -> item(FontAwesome.codeFork(), item.value)
                    RepositoryEntryType.TAGS -> item(FontAwesome.tags(), item.value)
                    RepositoryEntryType.TAG -> item(FontAwesome.tag(), item.value)
                    RepositoryEntryType.STASH -> item(FontAwesome.cubes(), item.value)
                    RepositoryEntryType.STASH_ENTRY -> item(FontAwesome.cube(), item.value)
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
                setOnAction { SettingsDialog(item.repository, scene.window).show() }
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
