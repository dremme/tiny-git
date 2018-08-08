package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Remote
import hamburg.remme.tinygit.system.git.Result
import org.springframework.stereotype.Service

/**
 * A service responsible for Git actions.
 */
@Service class GitService(private val log: Log, private val remote: Remote) {

    private var logCache: Result? = null

    /**
     * Invalidates the log cache.
     */
    fun invalidateCache() {
        logCache = null
    }

    /**
     * @return all commits in the current repository.
     */
    fun list(): Result {
        if (logCache == null) logCache = log.query()
        return logCache!!
    }

    /**
     * @return the count of all commits in the current repository.
     */
    fun count(): Int {
        return list().size
    }

    /**
     * Will perform a fetch and a pull if possible.
     */
    fun update() {
        remote.pull()
    }

}
