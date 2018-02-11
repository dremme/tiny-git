package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asFile
import hamburg.remme.tinygit.domain.ClientVersion
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.measureTime
import java.util.Scanner

val IC = RegexOption.IGNORE_CASE
val G = RegexOption.DOT_MATCHES_ALL
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

fun git(vararg args: String, block: (String) -> Unit) {
    measureTime("", args.joinToString(" ")) {
        val process = exec(args = *args)
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) block.invoke(it.nextLine()) }
    }
}

fun git(input: Array<String>, vararg args: String, block: (String) -> Unit) {
    measureTime("", args.joinToString(" ")) {
        val process = exec(args = *args)
        process.outputStream.bufferedWriter().use { it.write(input.joinToString("\n")) }
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) block.invoke(it.nextLine()) }
    }
}

fun git(repository: Repository, vararg args: String, block: (String) -> Unit) {
    measureTime(repository.shortPath, args.joinToString(" ")) {
        val process = exec(repository.path, *args)
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) block.invoke(it.nextLine()) }
    }
}

fun git(vararg args: String): String {
    return measureTime("", args.joinToString(" ")) {
        val process = exec(args = *args)
        val output = StringBuilder()
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) output.appendln(it.nextLine()) }
        output.toString()
    }
}

fun git(input: Array<String>, vararg args: String): String {
    return measureTime("", args.joinToString(" ")) {
        val process = exec(args = *args)
        process.outputStream.bufferedWriter().use { it.write(input.joinToString("\n")) }
        val output = StringBuilder()
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) output.appendln(it.nextLine()) }
        output.toString()
    }
}

fun git(repository: Repository, vararg args: String): String {
    return measureTime(repository.shortPath, args.joinToString(" ")) {
        val process = exec(repository.path, *args)
        val output = StringBuilder()
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) output.appendln(it.nextLine()) }
        output.toString()
    }
}

private fun exec(path: String? = null, vararg args: String): Process {
    val processBuilder = ProcessBuilder("git", *args.filter { it.isNotBlank() }.toTypedArray())
    path?.let { processBuilder.directory(it.asFile()) }
    processBuilder.redirectErrorStream(true)
    return processBuilder.start()
}
