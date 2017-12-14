package hamburg.remme.tinygit.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.git.LocalRepository

abstract class Refreshable {

    init {
        State.addRepositoryListener { it?.let { onRepositoryChanged(it) } ?: onRepositoryDeselected() }
        State.addRefreshListener { onRefresh(it) }
    }

    abstract fun onRefresh(repository: LocalRepository)

    abstract fun onRepositoryChanged(repository: LocalRepository)

    abstract fun onRepositoryDeselected()

}
