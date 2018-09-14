package hamburg.remme.tinygit.event

import java.io.File

/**
 * When a Git repository has been opened.
 * @property directory the path to the Git repository.
 */
class RepositoryOpenedEvent(val directory: File) : Event()

/**
 * When a Git repository has been closed.
 */
class RepositoryClosedEvent : Event()

/**
 * When a Git repository should be updated.
 */
class RepositoryUpdateRequestedEvent(val directory: File) : Event()

/**
 * When a Git repository has been updated (fetch & pull).
 */
class RepositoryUpdatedEvent(val directory: File) : Event()
