package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.Git

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
        State.aheadDefault.set(Git.divergenceDefault(repository))
        val (ahead, behind) = Git.divergence(repository)
        State.ahead.set(ahead)
        State.behind.set(behind)
    }

}
