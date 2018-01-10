package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.LocalRepository

abstract class Refreshable {

    init {
        State.addRepositoryListener { it?.let { onRepositoryChanged(it) } ?: onRepositoryDeselected() }
        State.addRefreshListener(this) { onRefresh(it) }
    }

    abstract fun onRefresh(repository: LocalRepository)

    abstract fun onRepositoryChanged(repository: LocalRepository)

    abstract fun onRepositoryDeselected()

}
