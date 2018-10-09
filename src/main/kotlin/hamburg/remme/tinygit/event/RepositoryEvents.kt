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
 * @property directory the path to the Git repository.
 */
class RepositoryUpdateRequestedEvent(val directory: File) : Event()

/**
 * When a Git repository has been updated (fetch & pull).
 * @property directory the path to the Git repository.
 */
class RepositoryUpdatedEvent(val directory: File) : Event()
