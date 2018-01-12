package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.Git
import javafx.collections.ListChangeListener

object MergeService : Refreshable() {

    init {
        val listener = ListChangeListener<GitFile> {
            if (State.isMerging.get() && State.stagedFiles.isEmpty() && State.pendingFiles.isEmpty()) State.isMerging.set(false)
        }
        State.stagedFiles.addListener(listener)
        State.pendingFiles.addListener(listener)
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.isMerging.set(false)
    }

    private fun update(repository: Repository) {
        State.isMerging.set(Git.isMerging(repository))
    }

}
