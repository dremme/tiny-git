package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.State
import hamburg.remme.tinygit.domain.Repository

abstract class Refreshable {

    init {
        State.addRepositoryListener { it?.let { onRepositoryChanged(it) } ?: onRepositoryDeselected() }
        State.addRefreshListener(this) { onRefresh(it) }
    }

    abstract fun onRefresh(repository: Repository)

    abstract fun onRepositoryChanged(repository: Repository)

    abstract fun onRepositoryDeselected()

}
