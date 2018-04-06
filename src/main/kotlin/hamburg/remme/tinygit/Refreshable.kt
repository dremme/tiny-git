package hamburg.remme.tinygit

import hamburg.remme.tinygit.domain.Repository

/**
 * Marker interface used in conjunction with the [Service] annotation.
 * The component scanner will automatically add the necessary listeners for the instance.
 */
interface Refreshable {

    /**
     * Called when [TinyGit.fireEvent] was triggered.
     * However it may not be called when [repository] is `null`.
     *
     * @param repository the current repository
     */
    fun onRefresh(repository: Repository)

    /**
     * Called when the active repository has changed.
     *
     * @param repository the new selected repository
     */
    fun onRepositoryChanged(repository: Repository)

    /**
     * Called when there is no more active repository.
     */
    fun onRepositoryDeselected()

}
