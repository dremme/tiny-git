package hamburg.remme.tinygit.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.api.Git

object StashService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.stashEntries.set(0)
    }

    private fun update(repository: LocalRepository) {
        State.stashEntries.set(Git.stashListSize(repository))
    }

}
