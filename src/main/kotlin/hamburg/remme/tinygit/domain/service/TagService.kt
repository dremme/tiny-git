package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitTagList
import hamburg.remme.tinygit.observableList
import javafx.concurrent.Task

class TagService : Refreshable {

    val tags = observableList<String>()
    private lateinit var repository: Repository
    private var task: Task<*>? = null

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        onRepositoryDeselected()
        update(repository)
    }

    override fun onRepositoryDeselected() {
        task?.cancel()
        tags.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<List<String>>() {
            override fun call() = gitTagList(repository)

            override fun succeeded() {
                tags.setAll(value)
            }
        }.also { TinyGit.execute(it) }
    }

}
