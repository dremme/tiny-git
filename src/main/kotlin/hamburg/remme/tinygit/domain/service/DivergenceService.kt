package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.git.gitDivergence
import hamburg.remme.tinygit.git.gitDivergenceExclusive
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task

object DivergenceService : Refreshable {

    val aheadDefault = SimpleIntegerProperty()
    val ahead = SimpleIntegerProperty()
    val behind = SimpleIntegerProperty()
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
        aheadDefault.set(0)
        ahead.set(0)
        behind.set(0)
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<Divergence>() {
            private var value1: Int = 0

            override fun call(): Divergence {
                value1 = gitDivergenceExclusive(repository)
                return gitDivergence(repository)
            }

            override fun succeeded() {
                aheadDefault.set(value1)
                ahead.set(value.ahead)
                behind.set(value.behind)
            }
        }.also { State.execute(it) }
    }

}
