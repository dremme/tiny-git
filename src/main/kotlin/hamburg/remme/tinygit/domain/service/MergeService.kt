package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalFile
import hamburg.remme.tinygit.domain.LocalRepository
import hamburg.remme.tinygit.git.Git
import javafx.collections.ListChangeListener

object MergeService : Refreshable() {

    init {
        val listener = ListChangeListener<LocalFile> {
            if (State.isMerging.get() && State.stagedFiles.isEmpty() && State.pendingFiles.isEmpty()) State.isMerging.set(false)
        }
        State.stagedFiles.addListener(listener)
        State.pendingFiles.addListener(listener)
    }

    override fun onRefresh(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: LocalRepository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        State.isMerging.set(false)
    }

    private fun update(repository: LocalRepository) {
        State.isMerging.set(Git.isMerging(repository))
    }

}
