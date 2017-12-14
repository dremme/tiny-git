package hamburg.remme.tinygit.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git

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
        State.rebasing.set(false)
    }

    private fun update(repository: LocalRepository) {
        val (next, last) = Git.rebaseState(repository)
        State.rebaseNext.set(next)
        State.rebaseLast.set(last)
        State.rebasing.set(Git.isRebasing(repository))
    }

}
