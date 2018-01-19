package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitIsMerging
import hamburg.remme.tinygit.git.gitMerge
import hamburg.remme.tinygit.git.gitMergeAbort
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.concurrent.Task

object MergeService : Refreshable {

    val isMerging = SimpleBooleanProperty(false)
    private lateinit var repository: Repository

    init {
        val listener = ListChangeListener<File> {
            if (isMerging.get() && WorkingCopyService.staged.isEmpty() && WorkingCopyService.pending.isEmpty()) isMerging.set(false)
        }
        WorkingCopyService.staged.addListener(listener)
        WorkingCopyService.pending.addListener(listener)
    }

    fun merge(mergeBase: String) {
        State.startProcess("Merging...", object : Task<Unit>() {
            override fun call() = gitMerge(repository, mergeBase)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()
        })
    }

    fun abort() {
        State.startProcess("Aborting...", object : Task<Unit>() {
            override fun call() = gitMergeAbort(repository)

            override fun succeeded() = State.fireRefresh()

            override fun failed() = exception.printStackTrace()
        })
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryDeselected() {
        isMerging.set(false)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        isMerging.set(gitIsMerging(repository))
    }

}
