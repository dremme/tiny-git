package hamburg.remme.tinygit.system

import java.io.File
import java.lang.System.arraycopy

/**
 * Responsible for native console IO.
 */
object Console {

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. All lines printed by the
     * command will be returned as one [String].
     *
     * @param args the git command and arguments. Must not be empty.
     */
    fun git(vararg args: String): String {
        val builder = OutputBuilder()
        execute(null, prependGit(*args), builder::append)
        return builder.toString().trim()
    }

    /**
     * Will execute the given arguments as git-command, prepending `git` before all arguments. Each lines printed by the
     * command will invoke the given block.
     *
     * @param args  the git command and arguments. Must not be empty.
     * @param block invoked for each printed line.
     */
    fun git(vararg args: String, block: (String) -> Unit) {
        execute(null, prependGit(*args), block)
    }

    private fun prependGit(vararg args: String): Array<String> {
        val command = Array(args.size + 1) { "" }
        command[0] = "git"
        arraycopy(args, 0, command, 1, args.size)
        return command
    }

    /**
     * Will execute the given arguments as shell command in the given working directory (if any). For each printed line
     * the consumer is being called.
     *
     * @param workingDir if `null` the current working directory is being used.
     * @param args       the arguments. The first argument is usually the command.
     * @param block      invoked for each printed line.
     */
    fun execute(workingDir: String?, args: Array<String>, block: (String) -> Unit) {
        ProcessBuilder(*args)
                .redirectErrorStream(true)
                .directory(workingDir?.let(::File))
                .start()
                .inputStream
                .bufferedReader()
                .useLines { it.forEach(block) }
    }

    /**
     * Private [StringBuilder] used for reading console output.
     */
    private class OutputBuilder {

        private val builder = StringBuilder()

        fun append(text: String) {
            builder.appendln(text)
        }

        override fun toString() = builder.toString()

    }

}
