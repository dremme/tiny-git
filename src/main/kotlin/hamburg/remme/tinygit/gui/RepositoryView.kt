package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.collections.ListChangeListener
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import org.eclipse.jgit.api.errors.CheckoutConflictException

class RepositoryView : TreeView<RepositoryView.RepositoryEntry>() {

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it ->
            it?.let { State.setSelectedRepository(it.value.repository) }
        }

        val repositories = State.getRepositories()
        repositories.addListener(ListChangeListener {
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
        repositories.forEach { addRepo(it) }
        selectionModel.selectFirst()

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
        State.addRefreshListener {
            // TODO: prob needs to refresh all repos or refresh on selection
            refreshRepo(State.getSelectedRepository())
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

        val branchList = LocalGit.branchListAll(repository)
        localBranches.children.addAll(branchList.filter { !it.remote }.map {
            TreeItem(RepositoryEntry(repository, it.shortRef,
                    if (it.current) RepositoryEntryType.CURRENT_BRANCH else RepositoryEntryType.LOCAL_BRANCH))
        })
        remoteBranches.children.addAll(branchList.filter { it.remote }.map {
            TreeItem(RepositoryEntry(repository, it.shortRef, RepositoryEntryType.REMOTE_BRANCH))
        })

        val stashEntries = LocalGit.stashList(repository)
        stash.children.addAll(stashEntries.map {
            TreeItem(RepositoryEntry(repository, it.message, RepositoryEntryType.STASH_ENTRY))
        })

        repoTree.children.addAll(localBranches, remoteBranches, tags, stash)
        root.children += repoTree
    }

    private fun removeRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let { root.children -= it }
    }

    private fun refreshRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let {
            // TODO: not refreshing remote branches yet (e.g. after fetch --prune)
            // TODO: selection might get lost on removed branches (e.g. after pruning)
            val selected = selectionModel.selectedIndex
            val branchList = LocalGit.branchListAll(repository)
            it.children[0].children.setAll(branchList.filter { !it.remote }.map {
                TreeItem(RepositoryEntry(repository, it.shortRef,
                        if (it.current) RepositoryEntryType.CURRENT_BRANCH else RepositoryEntryType.LOCAL_BRANCH))
            })
            selectionModel.select(selected)
        }
    }

    private fun checkout(repository: LocalRepository, branch: String) {
        State.addProcess("Switching branches...")
        State.execute(object : Task<Unit>() {
            override fun call() = LocalGit.checkout(repository, branch)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                when (exception) {
                    is CheckoutConflictException -> errorAlert(scene.window,
                            "Cannot Switch Branches",
                            "There are local changes that would be overwritten by checkout.\nCommit or stash them.")
                    else -> exception.printStackTrace()
                }
            }

            override fun done() = State.removeProcess()
        })
    }

    private fun checkoutRemote(repository: LocalRepository, branch: String) {
        State.addProcess("Getting remote branch...")
        State.execute(object : Task<Unit>() {
            override fun call() = LocalGit.checkoutRemote(repository, branch)

            override fun succeeded() = State.fireRefresh()

            override fun failed() {
                exception.printStackTrace()
            }

            override fun done() = State.removeProcess()
        })
    }

    class RepositoryEntry(val repository: LocalRepository, val value: String, val type: RepositoryEntryType) {

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
        LOCAL, LOCAL_BRANCH, CURRENT_BRANCH,
        REMOTE, REMOTE_BRANCH,
        TAGS, TAG,
        STASH, STASH_ENTRY

    }

    private class RepositoryEntryListCell : TreeCell<RepositoryEntry>() {

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                // TODO: clean-up this mess
                when (item!!.type) {
                    RepositoryEntryType.REPOSITORY -> repositoryBox(item)
                    RepositoryEntryType.LOCAL -> HBox(FontAwesome.desktop(), Label(item.value))
                    RepositoryEntryType.REMOTE -> HBox(FontAwesome.cloud(), Label(item.value))
                    RepositoryEntryType.LOCAL_BRANCH -> HBox(FontAwesome.codeFork(), Label(item.value))
                    RepositoryEntryType.REMOTE_BRANCH -> HBox(FontAwesome.codeFork(), Label(item.value))
                    RepositoryEntryType.CURRENT_BRANCH -> currentBox(item)
                    RepositoryEntryType.TAGS -> HBox(FontAwesome.tags(), Label(item.value))
                    RepositoryEntryType.TAG -> HBox(FontAwesome.tag(), Label(item.value))
                    RepositoryEntryType.STASH -> HBox(FontAwesome.cubes(), Label(item.value))
                    RepositoryEntryType.STASH_ENTRY -> HBox(FontAwesome.cube(), Label(item.value))
                }.addClass("repository-cell")
            }
        }

        private fun repositoryBox(item: RepositoryEntry)
                = HBox(Label(item.value).addStyle("-fx-font-weight:bold"),
                button(icon = FontAwesome.cog(),
                        styleClass = "settings",
                        action = EventHandler { SettingsDialog(item.repository, scene.window).show() }))

        private fun currentBox(item: RepositoryEntry)
                = HBox(FontAwesome.codeFork(), Label(item.value), FontAwesome.check()).addClass("current")

    }

}
