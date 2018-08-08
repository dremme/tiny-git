package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Result
import org.springframework.stereotype.Service

/**
 * A service responsible for Git actions.
 */
@Service class GitService(private val log: Log) {

    private var logCache: Result? = null

    /**
     * Lists all commits in the current repository.
     */
    fun list(): Result {
        if (logCache == null) logCache = log.query()
        return logCache!!
    }

    /**
     * Counts all commits in the current repository.
     */
    fun count(): Int {
        return list().size
    }

    /**
     * Invalidates the log cache.
     */
    fun invalidateCache() {
        logCache = null
    }

}
