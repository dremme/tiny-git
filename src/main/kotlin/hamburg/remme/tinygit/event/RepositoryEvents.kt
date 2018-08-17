package hamburg.remme.tinygit.event

import java.io.File

/**
 * When a Git repository has been opened.
 * @property directory the path to the Git repository.
 */
data class RepositoryOpenedEvent(val directory: File)

/**
 * When a Git repository has been closed.
 */
class RepositoryClosedEvent
