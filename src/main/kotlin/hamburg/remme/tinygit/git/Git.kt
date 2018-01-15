package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asFile
import hamburg.remme.tinygit.decrypt
import hamburg.remme.tinygit.domain.ClientVersion
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.measureTime
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Scanner
import org.eclipse.jgit.api.Git as JGit
import org.eclipse.jgit.lib.Repository as JGitRepository

val IC = RegexOption.IGNORE_CASE
val G = RegexOption.DOT_MATCHES_ALL
val errorSeparator = "error: "
val fatalSeparator = "fatal: "
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

fun git(repository: Repository, vararg args: String): String {
    return measureTime(repository.shortPath, args.joinToString(" ")) {
        val process = exec(repository.path, *args)
        val output = StringBuilder()
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) output.appendln(it.nextLine()) }
        output.toString()
    }
}

private fun exec(path: String? = null, vararg args: String): Process {
    val processBuilder = ProcessBuilder("git", *args)
    path?.let { processBuilder.directory(it.asFile()) }
    processBuilder.redirectErrorStream(true)
    return processBuilder.start()
}

object Git {

    private val REMOTE = Constants.DEFAULT_REMOTE_NAME
    private var proxyHost = ThreadLocal<String>() // TODO: really needed? cannot interact with more than one repo atm
    private var proxyPort = ThreadLocal<Int>() // TODO: really needed? cannot interact with more than one repo atm

    init {
        ProxySelector.setDefault(object : ProxySelector() {
            private val delegate = ProxySelector.getDefault()

            override fun select(uri: URI): List<Proxy> {
                return if (proxyHost.get().isNotBlank()) listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost.get(), proxyPort.get())))
                else delegate.select(uri)
            }

            override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) = Unit
        })
    }

    /**
     * - git log master..
     */
    fun logWithoutDefault(repository: Repository): List<Commit> {
        return repository.open("prep squash") {
            val head = it.resolve(it.branch)
            val defaultIds = defaultBranches.mapNotNull(it::findRef).map { it.objectId }
            it.revWalk().use {
                defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                it.markStart(it.parseCommit(head))
                it.map { it.toLocalCommit() }
            }
        }
    }

    private fun RevCommit.toLocalCommit() = Commit(
            name, abbreviate(10).name(),
            parents.map { it.name }, parents.map { it.abbreviate(10).name() },
            fullMessage, shortMessage,
            commitTime(),
            authorIdent.name,
            authorIdent.emailAddress)

    private fun RevCommit.commitTime()
            = LocalDateTime.ofEpochSecond(commitTime.toLong(), 0,
            ZoneOffset.ofTotalSeconds(authorIdent.timeZoneOffset * 60))

    /**
     * - git status
     */
    fun divergence(repository: Repository): Divergence {
        return repository.open("divergence") {
            val localBranch = it.resolve(it.branch)
            val remoteBranch = it.resolve("$REMOTE/${it.branch}")
            when {
                localBranch == null -> Divergence(0, 0)
                remoteBranch == null -> {
                    val defaultIds = defaultBranches.mapNotNull(it::findRef).map { it.objectId }
                    it.revWalk().use {
                        defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                        it.markStart(it.parseCommit(localBranch))
                        val ahead = it.count().takeIf { it > 0 } ?: -1
                        Divergence(ahead, 0)
                    }
                }
                else -> it.revWalk().use {
                    val localCommit = it.parseCommit(localBranch)
                    val remoteCommit = it.parseCommit(remoteBranch)
                    it.revFilter = RevFilter.MERGE_BASE
                    it.markStart(localCommit)
                    it.markStart(remoteCommit)
                    val mergeBase = it.next()
                    it.reset()
                    it.revFilter = RevFilter.ALL
                    Divergence(
                            RevWalkUtils.count(it, localCommit, mergeBase),
                            RevWalkUtils.count(it, remoteCommit, mergeBase))
                }
            }
        }
    }

    /**
     * - git status
     */
    // TODO: redundancy with logWithoutDefault
    fun divergenceDefault(repository: Repository): Int {
        return repository.open("divergence default") {
            val head = it.resolve(it.branch) ?: return 0
            val defaultIds = defaultBranches.mapNotNull(it::findRef).map { it.objectId }
            it.revWalk().use {
                defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                it.markStart(it.parseCommit(head))
                it.count()
            }
        }
    }

    /**
     * - git rebase --interactive <[baseId]>
     */
    fun rebaseSquash(repository: Repository, baseId: String, message: String) {
        repository.openGit("squash") {
            val result = it.rebase().setUpstream(ObjectId.fromString(baseId))
                    .runInteractively(object : RebaseCommand.InteractiveHandler {
                        override fun prepareSteps(steps: List<RebaseTodoLine>) {
                            steps.drop(1).forEach { it.action = RebaseTodoLine.Action.SQUASH }
                        }

                        override fun modifyCommitMessage(commit: String) = message
                    })
                    .call()
            if (result.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
                throw PrepareSquashException("There are uncommitted changes on ${it.repository.branch}.")
            } else if (!result.status.isSuccessful) {
                it.rebase().setOperation(RebaseCommand.Operation.ABORT).call()
                throw SquashException("Could not squash commits of branch ${it.repository.branch}")
            }
        }
    }

    /**
     * - git init
     */
    fun init(path: String): Repository {
        JGit.init().setDirectory(path.asFile()).call()
        return Repository(path)
    }

    /**
     * - git clone <[url]>
     */
    fun clone(repository: Repository, url: String) {
        JGit.cloneRepository().applyAuth(repository).setDirectory(repository.path.asFile()).setURI(url).call()
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: Repository): C {
        return if (repository.ssh.isNotBlank()) setTransportConfigCallback(TransportCallback(repository.ssh, repository.password))
        else setCredentialsProvider(UsernamePasswordCredentialsProvider(repository.username, repository.password.decrypt()))
    }

    private inline fun <T> Repository.open(description: String = "", block: (JGitRepository) -> T): T {
        Git.proxyHost.set(proxyHost)
        Git.proxyPort.set(proxyPort)
        val key = RepositoryCache.FileKey.lenient(path.asFile(), FS.DETECTED)
        return measureTime(shortPath, description) {
            RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build().let(block)
        }
    }

    private inline fun <T> Repository.openGit(description: String = "", block: (JGit) -> T) = open(description) { JGit(it).let(block) }

    private fun JGitRepository.revWalk() = RevWalk(this)

}
