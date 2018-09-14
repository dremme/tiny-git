package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.event.RepositoryUpdateRequestedEvent
import hamburg.remme.tinygit.event.RepositoryUpdatedEvent
import hamburg.remme.tinygit.logger
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.system.git.GitLog
import hamburg.remme.tinygit.system.git.GitRemote
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.io.File

/**
 * A service responsible for repository actions.
 */
@Service class RepositoryService(private val gitLog: GitLog,
                                 private val gitRemote: GitRemote,
                                 private val publisher: ApplicationEventPublisher) {

    private val log = logger<RepositoryService>()

    /**
     * @param gitDir a local Git repository.
     * @return all commits in the current repository.
     */
    fun list(gitDir: File): List<Commit> {
        return gitLog.query(gitDir)
    }

    /**
     * Will perform a fetch and a pull if possible. Will also invalidate the log cache.
     * @param gitDir a local Git repository.
     * @todo should this only be triggerable by event?
     */
    fun update(gitDir: File) {
        gitRemote.pull(gitDir)
        gitLog.invalidateCache()
    }

    /**
     * Handles a repository update requested.
     * @param event the event.
     */
    @EventListener fun handleRequestUpdate(event: RepositoryUpdateRequestedEvent) {
        update(event.directory)
        log.info("${event.directory} successfully updated.")
        publisher.publishEvent(RepositoryUpdatedEvent(event.directory))
    }

}
