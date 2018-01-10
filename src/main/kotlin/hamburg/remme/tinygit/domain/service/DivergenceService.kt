package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalRepository
import hamburg.remme.tinygit.git.Git

object DivergenceService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.aheadDefault.set(0)
        State.ahead.set(0)
        State.behind.set(0)
    }

    private fun update(repository: LocalRepository) {
        State.aheadDefault.set(Git.divergenceDefault(repository))
        val (ahead, behind) = Git.divergence(repository)
        State.ahead.set(ahead)
        State.behind.set(behind)
    }

}
