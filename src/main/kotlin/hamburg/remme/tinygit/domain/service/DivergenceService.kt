package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDivergence
import hamburg.remme.tinygit.git.gitDivergenceExclusive

object DivergenceService : Refreshable() {

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.aheadDefault.set(0)
        State.ahead.set(0)
        State.behind.set(0)
    }

    private fun update(repository: Repository) {
        State.aheadDefault.set(gitDivergenceExclusive(repository))
        val (ahead, behind) = gitDivergence(repository)
        State.ahead.set(ahead)
        State.behind.set(behind)
    }

}
