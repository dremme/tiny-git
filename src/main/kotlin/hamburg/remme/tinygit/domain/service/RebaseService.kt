package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitIsRebasing
import hamburg.remme.tinygit.git.gitRebaseStatus

object RebaseService : Refreshable() {

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.rebaseNext.set(0)
        State.rebaseLast.set(0)
        State.isRebasing.set(false)
    }

    private fun update(repository: Repository) {
        val (next, last) = gitRebaseStatus(repository)
        State.rebaseNext.set(next)
        State.rebaseLast.set(last)
        State.isRebasing.set(gitIsRebasing(repository))
    }

}
