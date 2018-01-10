package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalRepository
import hamburg.remme.tinygit.git.Git

object StashService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.stashSize.set(0)
    }

    private fun update(repository: LocalRepository) {
        State.stashSize.set(Git.stashListSize(repository))
    }

}
