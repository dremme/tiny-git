package hamburg.remme.tinygit.system

import hamburg.remme.tinygit.CURRENT_DIR
import java.io.File

/**
 * Responsible for native console IO.
 */
object Console {

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. All lines printed by the
     * command will be returned as one [String].
     * @param args the git command and arguments. Must not be empty.
     */
    fun git(vararg args: String): String {
        val builder = OutputBuilder()
        execute(args = prependGit(*args), block = builder::append)
        return builder.toString().trim()
    }

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. Each lines printed by the
     * command will invoke the given block.
     * @param args  the git command and arguments. Must not be empty.
     * @param block invoked for each printed line.
     */
    fun git(vararg args: String, block: (String) -> Unit) {
        execute(args = prependGit(*args), block = block)
    }

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. All lines printed by the
     * command will be returned as one [String].
     * @param gitDir a local Git repository. If the path is `.` the current working directory will be expected to be a Git
     *               repository.
     * @param args   the git command and arguments. Must not be empty.
     */
    fun git(gitDir: File, vararg args: String): String {
        val builder = OutputBuilder()
        execute(gitDir, prependGit(*args), builder::append)
        return builder.toString().trim()
    }

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. Each lines printed by the
     * command will invoke the given block.
     * @param gitDir a local Git repository. If the path is `.` the current working directory will be expected to be a Git
     *               repository.
     * @param args   the git command and arguments. Must not be empty.
     * @param block  invoked for each printed line.
     */
    fun git(gitDir: File, vararg args: String, block: (String) -> Unit) {
        execute(gitDir, prependGit(*args), block)
    }

    /**
     * Will execute the given arguments as shell command in the given working directory (if any). For each printed line
     * the consumer is being called.
     * @param workingDir the working directory to execute the command in. If the path is `.` the current working directory
     *                   will be used instead.
     * @param args       the arguments. The first argument is usually the command.
     * @param block      invoked for each printed line.
     */
    fun execute(workingDir: File = CURRENT_DIR, args: Array<String>, block: (String) -> Unit) {
        ProcessBuilder(*args)
          .redirectErrorStream(true)
          .directory(workingDir.takeIf { it != CURRENT_DIR }?.normalize())
          .start()
          .inputStream
          .bufferedReader()
          .useLines { it.forEach(block) }
    }

    private fun prependGit(vararg args: String): Array<String> {
        val command = Array(args.size + 1) { "" }
        command[0] = "git"
        System.arraycopy(args, 0, command, 1, args.size)
        return command
    }

    /**
     * Private [StringBuilder] used for reading console output.
     */
    private class OutputBuilder {

        private val builder = StringBuilder()

        fun append(text: String) {
            builder.appendln(text)
        }

        override fun toString(): String = builder.toString()

    }

}
