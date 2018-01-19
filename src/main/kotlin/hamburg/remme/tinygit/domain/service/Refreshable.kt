package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.domain.Repository

interface Refreshable {

    fun onRefresh(repository: Repository)

    fun onRepositoryChanged(repository: Repository)

    fun onRepositoryDeselected()

}
