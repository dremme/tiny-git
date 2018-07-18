package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Log
import org.springframework.stereotype.Service

/**
 * A service responsible for Git log actions.
 */
@Service class LogService {

    /**
     * Counts all commits in the current repository.
     */
    fun countAll(): Int {
        return Log.count()
    }

}
