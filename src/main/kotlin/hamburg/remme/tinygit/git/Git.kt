package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asFile
import hamburg.remme.tinygit.domain.ClientVersion
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.measureTime
import java.io.BufferedReader
import java.io.InputStreamReader

val IC = RegexOption.IGNORE_CASE
val G = RegexOption.DOT_MATCHES_ALL
const val EMPTY_ID = "4b825dc642cb6eb9a060e54bf8d69288fbee4904" // TODO: right place here?
const val WARNING_SEPARATOR = "warning: "
const val ERROR_SEPARATOR = "error: "
const val FATAL_SEPARATOR = "fatal: "
val defaultBranches =
    arrayOf(
        "main",
        "origin/main",
        "master",
        "origin/master",
        "develop",
        "origin/develop",
        "trunk",
        "origin/trunk",
    )
private val gitVersion = arrayOf("version")
private val versionPattern = "git version (\\d+)\\.(\\d+)\\.(\\d+).*".toRegex(setOf(IC, G))

fun gitIsInstalled(): Boolean = git(*gitVersion).trim().contains(versionPattern)

fun gitVersion(): ClientVersion {
    val response = git(*gitVersion).trim()
    val match = versionPattern.matchEntire(response)!!.groupValues
    return ClientVersion(match[1].toInt(), match[2].toInt(), match[3].toInt())
}

fun git(
    vararg args: String,
    block: (String) -> Unit,
) = exec(args = args, block = block)

fun git(
    input: Array<String>,
    vararg args: String,
    block: (String) -> Unit,
) = exec(input = input, args = args, block = block)

fun git(
    repository: Repository,
    vararg args: String,
    block: (String) -> Unit,
) = exec(repository = repository, args = args, block = block)

fun git(vararg args: String): String {
    val output = StringBuilder()
    exec(args = args) { output.appendLine(it) }
    return output.toString()
}

fun git(
    input: Array<String>,
    vararg args: String,
): String {
    val output = StringBuilder()
    exec(input = input, args = args) { output.appendLine(it) }
    return output.toString()
}

fun git(
    repository: Repository,
    vararg args: String,
): String {
    val output = StringBuilder()
    exec(repository = repository, args = args) { output.appendLine(it) }
    return output.toString()
}

private fun exec(
    repository: Repository? = null,
    input: Array<String>? = null,
    args: Array<out String>,
    block: (String) -> Unit,
) {
    measureTime(repository?.shortPath ?: "", args.joinToString(" ")) {
        val builder = ProcessBuilder("git", *args.filter { it.isNotBlank() }.toTypedArray())
        builder.redirectErrorStream(true)
        builder.directory(repository?.path?.asFile())
        val process = builder.start()
        process.outputStream.bufferedWriter().use { it.write(input?.joinToString("\n") ?: "") }
        BufferedReader(InputStreamReader(process.inputStream)).use { it.lines().forEach(block) }
    }
}
