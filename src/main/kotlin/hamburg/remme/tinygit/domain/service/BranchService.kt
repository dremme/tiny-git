package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalRepository
import hamburg.remme.tinygit.git.Git

object BranchService : Refreshable() {

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.branchCount.set(0)
    }

    private fun update(repository: LocalRepository) {
        State.branchCount.set(Git.branchListAll(repository).size)
    }

}
