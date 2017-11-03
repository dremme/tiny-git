package hamburg.remme.tinygit.gui

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalGit
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.gui.dialog.SettingsDialog
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import org.kordamp.ikonli.fontawesome.FontAwesome

class RepositoryView(repositories: ObservableList<LocalRepository>) : TreeView<RepositoryView.RepositoryEntry>() {

    init {
        setCellFactory { RepositoryEntryListCell() }
        root = TreeItem()
        isShowRoot = false
        selectionModel.selectedItemProperty().addListener { _, _, it -> State.setSelectedRepository(it.value.repository) }

        repositories.addListener(ListChangeListener {
            while (it.next()) {
                when {
                    it.wasAdded() -> it.addedSubList.forEach { addRepo(it) }
                    it.wasRemoved() -> it.removed.forEach { removeRepo(it) }
                }
            }
        })
        repositories.forEach { addRepo(it) }
        selectionModel.selectFirst()
    }

    private fun addRepo(repository: LocalRepository) {
        val branches = TreeItem(RepositoryEntry(
                repository,
                repository.path.split("[\\\\/]".toRegex()).last(),
                RepositoryEntryType.REPOSITORY))

        val localBranches = TreeItem(RepositoryEntry(repository, "Local Branches", RepositoryEntryType.LOCAL))
        val remoteBranches = TreeItem(RepositoryEntry(repository, "Remote Branches", RepositoryEntryType.REMOTE))
        branches.children.addAll(localBranches, remoteBranches)

        val branchList = LocalGit.branchListAll(repository)
        localBranches.children.addAll(branchList.filter { !it.remote }.map {
            TreeItem(RepositoryEntry(repository, it.shortRef,
                    if (it.current) RepositoryEntryType.CURRENT_BRANCH else RepositoryEntryType.BRANCH))
        })
        remoteBranches.children.addAll(branchList.filter { it.remote }.map {
            TreeItem(RepositoryEntry(repository, it.shortRef,
                    if (it.current) RepositoryEntryType.CURRENT_BRANCH else RepositoryEntryType.BRANCH))
        })

        root.children += branches
    }

    private fun removeRepo(repository: LocalRepository) {
        root.children.find { it.value.repository == repository }?.let { root.children.remove(it) }
    }

    class RepositoryEntry(val repository: LocalRepository, val label: String, val type: RepositoryEntryType) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as RepositoryEntry

            if (label != other.label) return false
            if (repository != other.repository) return false

            return true
        }

        override fun hashCode(): Int {
            var result = label.hashCode()
            result = 31 * result + repository.hashCode()
            return result
        }

    }

    enum class RepositoryEntryType { REPOSITORY, LOCAL, REMOTE, BRANCH, CURRENT_BRANCH }

    private class RepositoryEntryListCell : TreeCell<RepositoryEntry>() {

        override fun updateItem(item: RepositoryEntry?, empty: Boolean) {
            super.updateItem(item, empty)
            graphic = if (empty) null else {
                when (item!!.type) {
                    RepositoryEntryType.REPOSITORY -> repositoryBox(item)
                    RepositoryEntryType.LOCAL -> HBox(icon(FontAwesome.DESKTOP), Label(item.label))
                    RepositoryEntryType.REMOTE -> HBox(icon(FontAwesome.CLOUD), Label(item.label))
                    RepositoryEntryType.BRANCH -> HBox(icon(FontAwesome.CODE_FORK), Label(item.label))
                    RepositoryEntryType.CURRENT_BRANCH -> currentBox(item)
                }.also { it.styleClass += "repository-cell" }
            }
        }

        private fun repositoryBox(item: RepositoryEntry)
                = HBox(Label(item.label).also { it.style = "-fx-font-weight:bold" },
                button(icon = icon(FontAwesome.COG),
                        styleClass = "settings",
                        action = EventHandler { SettingsDialog(item.repository, scene.window).show() }))

        private fun currentBox(item: RepositoryEntry)
                = HBox(icon(FontAwesome.CODE_FORK), Label(item.label), icon(FontAwesome.CHECK)).also { it.styleClass += "current" }

    }

}
