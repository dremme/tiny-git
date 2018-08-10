package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.system.Console
import org.springframework.stereotype.Component
import java.io.File

/**
 * Responsible for all remote actions.
 */
@Component class Remote {

    /**
     * Performs a pull which will also perform a fetch.
     *
     * @param gitDir a local Git repository.
     * @return the console output of the pull command.
     */
    fun pull(gitDir: File): String {
        return Console.git(gitDir, PULL, "--all", "--ff-only")
    }

}
