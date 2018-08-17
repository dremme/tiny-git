package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Remote
import org.springframework.stereotype.Service
import java.io.File

/**
 * A service responsible for repository actions.
 */
@Service class RepositoryService(private val log: Log, private val remote: Remote) {

    /**
     * @param gitDir a local Git repository.
     * @return all commits in the current repository.
     */
    fun list(gitDir: File): List<Commit> {
        return log.query(gitDir)
    }

    /**
     * @param gitDir a local Git repository.
     * @return the count of all commits in the current repository.
     */
    fun count(gitDir: File): Int {
        return log.query(gitDir).size
    }

    /**
     * Will perform a fetch and a pull if possible.
     * @param gitDir a local Git repository.
     */
    fun update(gitDir: File) {
        remote.pull(gitDir)
    }

}
