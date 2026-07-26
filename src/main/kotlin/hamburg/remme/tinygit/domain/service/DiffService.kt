package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.Refreshable
import hamburg.remme.tinygit.Service
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.NumStat
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDiff
import hamburg.remme.tinygit.git.gitDiffNumstat

@Service
class DiffService : Refreshable {
    private val renderer = DiffRenderer()
    private lateinit var repository: Repository

    fun diff(
        file: File,
        contextLines: Int,
    ): String {
        val rawDiff = gitDiff(repository, file, contextLines)
        return renderer.render(rawDiff)
    }

    fun diff(
        file: File,
        commit: Commit,
        contextLines: Int,
    ): String {
        val rawDiff = gitDiff(repository, file, commit, contextLines)
        return renderer.render(rawDiff)
    }

    fun numStats(cached: Boolean): List<NumStat> = gitDiffNumstat(repository, cached)

    fun numStats(commit: Commit): List<NumStat> = gitDiffNumstat(repository, commit)

    override fun onRefresh(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryChanged(repository: Repository) {
        this.repository = repository
    }

    override fun onRepositoryDeselected() {
    }
}
