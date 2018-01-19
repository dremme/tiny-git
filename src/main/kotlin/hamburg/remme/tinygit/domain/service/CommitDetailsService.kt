package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiffTree
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleStringProperty

object CommitDetailsService : Refreshable {

    val commitStatus = observableList<File>()
    val commitDetails = SimpleStringProperty()
    private val detailsRenderer = DetailsRenderer()
    private lateinit var repository: Repository

    init {
        CommitLogService.activeCommit.addListener { _, _, it -> update(it) }
    }

    override fun onRefresh(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryChanged(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryDeselected() {
    }

    private fun update(commit: Commit?) {
        commitDetails.set(detailsRenderer.render(commit))
        commit?.let { commitStatus.setAll(gitDiffTree(repository, it)) } ?: commitStatus.clear()
    }

}
