package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.system.Console

/**
 * Reads Git logs and returns commits and general log info.
 */
object Log {

    /**
     * Returns all commit IDs in the Git repository in order of commit creation.
     */
    fun query(): LogResult {
        val commits = arrayListOf<String>()
        Console.git("rev-list", "--all") { commits.add(it) }
        return LogResult(commits)
    }

    /**
     * Returns the count of commits in the Git repository.
     */
    fun count(): Int {
        return Console.git("rev-list", "--all", "--count").toInt()
    }

}
