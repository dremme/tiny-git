package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Remote
import hamburg.remme.tinygit.system.git.Result
import org.springframework.stereotype.Service
import java.io.File

/**
 * A service responsible for repository actions.
 */
@Service class RepositoryService(private val log: Log, private val remote: Remote) {

    private var logCache: Result? = null

    /**
     * Invalidates the log cache.
     */
    fun invalidateCache() {
        logCache = null
    }

    /**
     * @param gitDir a local Git repository.
     * @return all commits in the current repository.
     */
    fun list(gitDir: File): Result {
        if (logCache == null) logCache = log.query(gitDir)
        return logCache!!
    }

    /**
     * @param gitDir a local Git repository.
     * @return the count of all commits in the current repository.
     */
    fun count(gitDir: File): Int {
        return list(gitDir).size
    }

    /**
     * Will perform a fetch and a pull if possible.
     *
     * @param gitDir a local Git repository.
     */
    fun update(gitDir: File) {
        remote.pull(gitDir)
    }

}
