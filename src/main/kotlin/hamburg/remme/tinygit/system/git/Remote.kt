package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.system.Console
import org.springframework.stereotype.Component

/**
 * Responsible for all remote actions.
 */
@Component class Remote {

    /**
     * Performs a pull which will also perform a fetch.
     *
     * @return the console output of the pull command.
     */
    fun pull(): String {
        return Console.git(PULL, "--all", "--ff-only")
    }

}
