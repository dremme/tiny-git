package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitBranchAll

object BranchService : Refreshable() {

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.branchCount.set(0)
    }

    private fun update(repository: Repository) {
        State.branchCount.set(gitBranchAll(repository).size)
    }

}
