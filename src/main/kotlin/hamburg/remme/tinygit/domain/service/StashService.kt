package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitStashList

object StashService : Refreshable() {

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.stashSize.set(0)
    }

    private fun update(repository: Repository) {
        State.stashSize.set(gitStashList(repository).size)
    }

}
