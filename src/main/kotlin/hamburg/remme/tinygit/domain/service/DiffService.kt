package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.Refreshable
import hamburg.remme.tinygit.Service
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiff

@Service
class DiffService : Refreshable {

    private val renderer = DiffRenderer()
    private lateinit var repository: Repository

    fun diff(file: File, contextLines: Int): String {
        val rawDiff = gitDiff(repository, file, contextLines)
        return renderer.render(rawDiff)
    }

    fun diff(file: File, commit: Commit, contextLines: Int): String {
        val rawDiff = gitDiff(repository, file, commit, contextLines)
        return renderer.render(rawDiff)
    }

    override fun onRefresh(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryChanged(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryDeselected() {
    }

}
