package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asFile
import hamburg.remme.tinygit.domain.ClientVersion
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.measureTime
import java.util.Scanner

val IC = RegexOption.IGNORE_CASE
val G = RegexOption.DOT_MATCHES_ALL
const val emptyId = "4b825dc642cb6eb9a060e54bf8d69288fbee4904" // TODO: right place here?
const val errorSeparator = "error: "
const val fatalSeparator = "fatal: "
val defaultBranches = arrayOf("master", "origin/master", "develop", "origin/develop", "trunk", "origin/trunk")
private val gitVersion = arrayOf("version")
private val versionPattern = "git version (\\d+)\\.(\\d+)\\.(\\d+).*".toRegex(setOf(IC, G))

fun gitIsInstalled(): Boolean {
    return git(*gitVersion).trim().contains(versionPattern)
}

fun gitVersion(): ClientVersion {
    val response = git(*gitVersion).trim()
    val match = versionPattern.matchEntire(response)!!.groupValues
    return ClientVersion(match[1].toInt(), match[2].toInt(), match[3].toInt())
}

fun git(vararg args: String, block: (String) -> Unit) = exec(args = *args) { block(it) }

fun git(input: Array<String>, vararg args: String, block: (String) -> Unit) = exec(input = input, args = *args) { block(it) }

fun git(repository: Repository, vararg args: String, block: (String) -> Unit) = exec(repository, args = *args) { block(it) }

fun git(vararg args: String): String {
    val output = StringBuilder()
    exec(args = *args) { output.appendln(it) }
    return output.toString()
}

fun git(input: Array<String>, vararg args: String): String {
    val output = StringBuilder()
    exec(input = input, args = *args) { output.appendln(it) }
    return output.toString()
}

fun git(repository: Repository, vararg args: String): String {
    val output = StringBuilder()
    exec(repository, args = *args) { output.appendln(it) }
    return output.toString()
}

private fun exec(repository: Repository? = null, input: Array<String>? = null, vararg args: String, block: (String) -> Unit) {
    measureTime(repository?.shortPath ?: "", args.joinToString(" ")) {
        val builder = ProcessBuilder("git", *args.filter { it.isNotBlank() }.toTypedArray())
        builder.redirectErrorStream(true)
        builder.directory(repository?.path?.asFile())
        val process = builder.start()
        process.outputStream.bufferedWriter().use { it.write(input?.joinToString("\n") ?: "") }
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) block(it.nextLine()) }
    }
}
