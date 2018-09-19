package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.join
import hamburg.remme.tinygit.system.cmd
import org.springframework.stereotype.Component
import java.io.File

/**
 * Responsible for all remote actions.
 */
@Component class GitRemote {

    /**
     * Performs a pull which will also perform a fetch.
     * @param gitDir a local Git repository.
     * @return the console output of the pull command.
     */
    fun pull(gitDir: File): String {
        return cmd(gitDir, listOf(GIT, PULL, ALL, "--ff-only")).join()
    }

}
