package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalRepository
import hamburg.remme.tinygit.git.Git

object RebaseService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.rebaseNext.set(0)
        State.rebaseLast.set(0)
        State.isRebasing.set(false)
    }

    private fun update(repository: LocalRepository) {
        val (next, last) = Git.rebaseState(repository)
        State.rebaseNext.set(next)
        State.rebaseLast.set(last)
        State.isRebasing.set(Git.isRebasing(repository))
    }

}
