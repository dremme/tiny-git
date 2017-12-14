package hamburg.remme.tinygit.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git

object DivergenceService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.ahead.set(0)
        State.behind.set(0)
    }

    private fun update(repository: LocalRepository) {
        val (ahead, behind) = Git.divergence(repository)
        State.ahead.set(ahead)
        State.behind.set(behind)
    }

}
