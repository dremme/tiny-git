package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.File
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.MergeException
import hamburg.remme.tinygit.git.gitIsMerging
import hamburg.remme.tinygit.git.gitMerge
import hamburg.remme.tinygit.git.gitMergeAbort
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.concurrent.Task

class MergeService(service: WorkingCopyService) : Refreshable {

    val isMerging = SimpleBooleanProperty(false)
    private lateinit var repository: Repository

    init {
        val listener = ListChangeListener<File> {
            if (isMerging.get() && service.staged.isEmpty() && service.pending.isEmpty()) isMerging.set(false)
        }
        service.staged.addListener(listener)
        service.pending.addListener(listener)
    }

    fun merge(mergeBase: String, errorHandler: () -> Unit) {
        TinyGit.execute("Merging...", object : Task<Unit>() {
            override fun call() = gitMerge(repository, mergeBase)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is MergeException -> errorHandler.invoke()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun abort() {
        TinyGit.execute("Aborting...", object : Task<Unit>() {
            override fun call() = gitMergeAbort(repository)

            override fun succeeded() = TinyGit.fireEvent()

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
