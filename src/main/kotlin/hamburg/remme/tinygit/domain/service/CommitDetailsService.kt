package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiffTree
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

object CommitDetailsService : Refreshable {

    val commitStatus = observableList<File>()
    val commitDetails = SimpleStringProperty()
    private val detailsRenderer = DetailsRenderer()
    private lateinit var repository: Repository
    private var task: Task<*>? = null

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
        task?.cancel()
        commitDetails.set("")
        commitStatus.clear()
    }

    private fun update(commit: Commit?) {
        task?.cancel()
        commit?.let {
            task = object : Task<List<File>>() {
                override fun call() = gitDiffTree(repository, it)

                override fun succeeded() {
                    commitDetails.set(detailsRenderer.render(it))
                    commitStatus.setAll(value)
                }
            }.also { TinyGit.execute(it) }
        } ?: commitStatus.clear()
    }

}
