package hamburg.remme.tinygit.system

import hamburg.remme.tinygit.CURRENT_DIR
import java.io.BufferedReader
import java.io.File

/**
 * Will execute the given arguments as console command in the given working directory (if any).
 * @param workingDir the working directory to execute the command in. If the path is `.` the current working
 *                   directory will be used instead.
 * @param args       the arguments. The first argument is usually the command.
 * @return a reader returning all the output streamed from the console.
 */
fun cmd(workingDir: File, args: List<String>): BufferedReader {
    return ProcessBuilder(args)
      .redirectErrorStream(true)
      .directory(workingDir.takeIf { it != CURRENT_DIR }?.normalize())
      .start()
      .inputStream
      .bufferedReader()
}
