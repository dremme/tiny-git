package hamburg.remme.tinygit.git

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.atEndOfDay
import hamburg.remme.tinygit.decrypt
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Divergence
import hamburg.remme.tinygit.domain.GitFile
import hamburg.remme.tinygit.domain.Rebase
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.measureTime
import hamburg.remme.tinygit.read
import hamburg.remme.tinygit.readFirst
import hamburg.remme.tinygit.readLines
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.AndRevFilter
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.WindowCacheConfig
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.util.FS
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Scanner
import org.eclipse.jgit.api.Git as JGit
import org.eclipse.jgit.lib.Repository as JGitRepository

val defaultBranches = arrayOf("master", "origin/master", "develop", "origin/develop", "trunk", "origin/trunk")
private val proxy = arrayOf("config", "http.proxy")
private val unsetProxy = arrayOf("config", "--unset", "http.proxy")

fun gitProxy(repository: Repository) {
    if (repository.proxyHost.isNotBlank()) git(repository, *proxy, "${repository.proxyHost}:${repository.proxyPort}")
    else git(repository, *unsetProxy)
}

fun git(repository: Repository, vararg args: String, block: (String) -> Unit) {
    measureTime(repository.shortPath, args.joinToString(" ")) {
        val process = gitProcess(repository.path, *args)
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) block.invoke(it.nextLine()) }
    }
}

fun git(repository: Repository, vararg args: String): String {
    return measureTime(repository.shortPath, args.joinToString(" ")) {
        val process = gitProcess(repository.path, *args)
        val output = StringBuilder()
        Scanner(process.inputStream).use { while (process.isAlive) while (it.hasNext()) output.appendln(it.nextLine()) }
        output.toString()
    }
}

private fun gitProcess(path: String, vararg args: String): Process {
    val processBuilder = ProcessBuilder("git", *args)
    processBuilder.directory(File(path))
    processBuilder.redirectErrorStream(true)
    return processBuilder.start()
}

object Git {

    private val REMOTE = Constants.DEFAULT_REMOTE_NAME
    private val cache = mutableSetOf<Repository>()
    private var proxyHost = ThreadLocal<String>() // TODO: really needed? cannot interact with more than one repo atm
    private var proxyPort = ThreadLocal<Int>() // TODO: really needed? cannot interact with more than one repo atm

    init {
        val config = WindowCacheConfig()
        config.packedGitLimit = 524288000L
        config.deltaBaseCacheLimit = 524288000
        config.streamFileThreshold = 524288000
        config.install()

        ProxySelector.setDefault(object : ProxySelector() {
            private val delegate = ProxySelector.getDefault()

            override fun select(uri: URI): List<Proxy> {
                return if (proxyHost.get().isNotBlank()) listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost.get(), proxyPort.get())))
                else delegate.select(uri)
            }

            override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) = Unit
        })
    }

    fun isUpdated(repository: Repository) = cache.contains(repository)

    fun isMerging(repository: Repository) = repository.open { it.repositoryState == RepositoryState.MERGING }

    fun isRebasing(repository: Repository) = repository.open { it.repositoryState.isRebasing }

    /**
     * One of the following:
     * - git remote add origin <[url]>
     * - git remote set-url origin <[url]>
     */
    fun setRemote(repository: Repository, url: String) {
        repository.openGit("set remote $url") {
            if (it.remoteList().call().isNotEmpty()) {
                val cmd = it.remoteSetUrl()
                cmd.setName(REMOTE)
                cmd.setUri(URIish(url))
                cmd.call()
                cmd.setPush(true)
                cmd.call()
            } else {
                val cmd = it.remoteAdd()
                cmd.setName(REMOTE)
                cmd.setUri(URIish(url))
                cmd.call()
            }
        }
    }

    /**
     * - git remote rm origin
     */
    fun removeRemote(repository: Repository) {
        repository.openGit("remove remote") {
            val cmd = it.remoteRemove()
            cmd.setName(REMOTE)
            cmd.call()
        }
    }

    /**
     * - git branch --all
     * - git log --all --skip=[skip] --max-count=[max]
     */
    fun log(repository: Repository, skip: Int, max: Int): List<Commit> {
        return log(repository, skip, max, false)
    }

    /**
     * - git fetch
     * - git branch --all
     * - git log --all --skip=[skip] --max-count=[max]
     */
    fun logFetch(repository: Repository, skip: Int, max: Int): List<Commit> {
        return log(repository, skip, max, true)
    }

    private fun log(repository: Repository, skip: Int, max: Int, fetch: Boolean): List<Commit> {
        return repository.openGit("log $skip->${max + skip}") {
            if (fetch) it.fetch(repository, false)
            val logCommand = it.log().setSkip(skip).setMaxCount(max)
            it.branchListIds().forEach { logCommand.add(it) }
            logCommand.call().map { it.toLocalCommit() }
        }
    }

    /**
     * - git log --all --after=<[after]> --before=<[before]>
     */
    fun log(repository: Repository, after: LocalDate?, before: LocalDate?): List<Commit> {
        return repository.openGit("log stream") {
            val walk = it.revWalk()
            if (after != null && before != null) {
                walk.revFilter = AndRevFilter.create(after.toAfterFilter(), before.toBeforeFilter())
            } else if (after != null) {
                walk.revFilter = after.toAfterFilter()
            } else if (before != null) {
                walk.revFilter = before.toBeforeFilter()
            }
            it.branchListIds().map { walk.parseCommit(it) }.forEach { walk.markStart(it) }
            walk.map { it.toLocalCommit() }
        }
    }

    private fun LocalDate.toAfterFilter() = CommitTimeRevFilter.after(atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    private fun LocalDate.toBeforeFilter() = CommitTimeRevFilter.before(atEndOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())

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
     * - git log HEAD --max-count=1
     */
    // TODO: or use commit edit file
    fun headMessage(repository: Repository): String {
        return repository.open("head message") {
            val head = it.findRef(it.branch)
            it.revWalk().use { it.parseCommit(head.objectId) }.fullMessage
        }
    }

    private fun JGit.branchListIds(): List<AnyObjectId> {
        return branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map { it.objectId }
    }

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
     * - git rm <[files] != MISSING>
     * - git add <[files] != MISSING>
     */
    fun stage(repository: Repository, files: List<GitFile>) {
        repository.openGit("stage $files") { git ->
            files.filter { it.status == GitFile.Status.REMOVED }.let { if (it.isNotEmpty()) git.remove(it) }
            files.filter { it.status != GitFile.Status.REMOVED }.let { if (it.isNotEmpty()) git.add(it) }
        }
    }

    /**
     * - git rm <[removed]>
     * - git add .
     */
    fun stageAll(repository: Repository, removed: List<GitFile>) {
        repository.openGit("stage all") {
            if (removed.isNotEmpty()) it.remove(removed)
            it.addAll()
        }
    }

    /**
     * - git add --update .
     */
    fun updateAll(repository: Repository) {
        repository.openGit("update") { it.add().setUpdate(true).addFilepattern(".").call() }
    }

    private fun JGit.add(files: List<GitFile>) {
        val addCommand = add()
        files.forEach { addCommand.addFilepattern(it.path) }
        addCommand.call()
    }

    private fun JGit.addAll() {
        add().addFilepattern(".").call()
    }

    private fun JGit.remove(files: List<GitFile>) {
        val rmCommand = rm()
        files.forEach { rmCommand.addFilepattern(it.path) }
        rmCommand.call()
    }

    /**
     * - git reset
     */
    fun reset(repository: Repository) {
        repository.openGit("reset") { it.reset().call() }
    }

    /**
     * - git reset <[files]>
     */
    fun reset(repository: Repository, files: List<GitFile>) {
        repository.openGit("reset $files") {
            val resetCommand = it.reset()
            files.forEach { resetCommand.addPath(it.path) }
            resetCommand.call()
        }
    }

    /**
     * - git reset --hard origin/<current_branch>
     */
    fun resetHard(repository: Repository) {
        repository.openGit("reset hard") {
            it.reset().setMode(ResetCommand.ResetType.HARD).setRef("$REMOTE/${it.repository.branch}").call()
        }
    }

    /**
     * - git commit --message="[message]"
     */
    fun commit(repository: Repository, message: String) {
        commit(repository, message, false)
    }

    /**
     * - git commit --amend --message="[message]"
     */
    fun commitAmend(repository: Repository, message: String) {
        commit(repository, message, true)
    }

    private fun commit(repository: Repository, message: String, amend: Boolean) {
        repository.openGit("commit amend=$amend") { it.commit().setMessage(message).setAmend(amend).call() }
    }

//    /**
//     * - git push
//     */
//    fun push(repository: Repository) {
//        push(repository, false)
//    }
//
//    /**
//     * - git push --force
//     */
//    fun pushForce(repository: Repository) {
//        push(repository, true)
//    }
//
//    private fun push(repository: Repository, force: Boolean) {
//        repository.openGit("push force=$force") {
//            val result = it.push().applyAuth(repository).setForce(force).call()
//            if (result.count() > 0) {
//                result.first().remoteUpdates.forEach {
//                    when (it.status) {
//                        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> throw PushRejectedException(it.message ?: "")
//                        RemoteRefUpdate.Status.REJECTED_NODELETE -> throw DeleteRejectedException(it.message ?: "")
//                        RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> throw RemoteChangedException(it.message ?: "")
//                        RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> throw RejectedException(it.message ?: "")
//                        else -> Unit
//                    }
//                }
//            }
//        }
//    }

    /**
     * - git checkout -b [name]
     */
    fun branchCreate(repository: Repository, name: String) {
        repository.openGit("create $name") { it.checkout().setCreateBranch(true).setName(name).call() }
    }

    /**
     * - git branch --move [oldName] [newName]
     */
    fun branchRename(repository: Repository, oldName: String, newName: String) {
        repository.openGit("rename $oldName -> $newName") { it.branchRename().setOldName(oldName).setNewName(newName).call() }
    }

    /**
     * - git branch --delete [name]
     */
    fun branchDelete(repository: Repository, name: String) {
        branchDelete(repository, name, false)
    }

    /**
     * - git branch --delete --force [name]
     */
    fun branchDeleteForce(repository: Repository, name: String) {
        branchDelete(repository, name, true)
    }

    private fun branchDelete(repository: Repository, name: String, force: Boolean) {
        repository.openGit("delete force=$force $name") {
            it.branchDelete().setForce(force).setBranchNames(name).call()
        }
    }

    /**
     * - git merge <[branch]>
     */
    fun merge(repository: Repository, branch: String) {
        repository.openGit("merge $branch") {
            it.merge().include(it.repository.resolve(branch)).setMessage("Merged '$branch' into '${it.repository.branch}'.").call()
        }
    }

    /**
     * - git merge --abort
     */
    fun mergeAbort(repository: Repository) {
        repository.openGit("merge abort") {
            it.repository.writeMergeHeads(null)
            it.repository.writeMergeCommitMsg(null)
            it.reset().setMode(ResetCommand.ResetType.HARD).call()
        }
    }

    fun mergeMessage(repository: Repository) = "${repository.path}/.git/MERGE_MSG".asPath().read()

    /**
     * - git rebase <[branch]>
     */
    fun rebase(repository: Repository, branch: String) {
        repository.openGit("rebase $branch") { it.rebase().setUpstream(branch).call() }
    }

    /**
     * - git rebase --continue
     */
    fun rebaseContinue(repository: Repository) {
        repository.openGit("rebase continue") {
            val result = it.rebase().setOperation(RebaseCommand.Operation.CONTINUE).call()
            if (result.status == RebaseResult.Status.NOTHING_TO_COMMIT) {
                it.rebase().setOperation(RebaseCommand.Operation.SKIP).call()
            }
        }
    }

    /**
     * - git rebase --abort
     */
    fun rebaseAbort(repository: Repository) {
        repository.openGit("rebase abort") { it.rebase().setOperation(RebaseCommand.Operation.ABORT).call() }
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

    fun rebaseState(repository: Repository): Rebase {
        val state = repository.open { it.repositoryState }
        if (state.isRebasing) {
            return when (state) {
                RepositoryState.REBASING, RepositoryState.REBASING_REBASING -> rebaseStateApply(repository)
                RepositoryState.REBASING_MERGE, RepositoryState.REBASING_INTERACTIVE -> rebaseStateMerge(repository)
                else -> throw IllegalStateException("Unexpected state $state")
            }
        } else {
            return Rebase(0, 0)
        }
    }

    private fun rebaseStateApply(repository: Repository): Rebase {
        val rebasePath = "${repository.path}/.git/rebase-apply".asPath()
        val next = rebasePath.resolve("next").readFirst().toInt()
        val last = rebasePath.resolve("last").readFirst().toInt()
        return Rebase(next, last)
    }

    private fun rebaseStateMerge(repository: Repository): Rebase {
        val rebasePath = "${repository.path}/.git/rebase-merge".asPath()
        val done = rebasePath.resolve("done").readLines().countMeaningful()
        val todo = rebasePath.resolve("git-rebase-todo").readLines().countMeaningful()
        return Rebase(done, done + todo)
    }

    private fun List<String>.countMeaningful() = filterNot { it.isBlank() || it.startsWith("#") }.size

    private fun JGit.fetch(repository: Repository, prune: Boolean) {
        if (!isUpdated(repository)) {
            fetch().applyAuth(repository).setRemoveDeletedRefs(prune).call()
            cache += repository
        }
    }

    /**
     * - git init
     */
    fun init(path: File): Repository {
        JGit.init().setDirectory(path).call()
        return Repository(path.absolutePath)
    }

    /**
     * - git clone <[url]>
     */
    fun clone(repository: Repository, url: String) {
        JGit.cloneRepository().applyAuth(repository).setDirectory(File(repository.path)).setURI(url).call()
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: Repository): C {
        return if (repository.ssh.isNotBlank()) setTransportConfigCallback(TransportCallback(repository.ssh, repository.password))
        else setCredentialsProvider(UsernamePasswordCredentialsProvider(repository.username, repository.password.decrypt()))
    }

    private inline fun <T> Repository.open(description: String = "", block: (JGitRepository) -> T): T {
        Git.proxyHost.set(proxyHost)
        Git.proxyPort.set(proxyPort)
        val key = RepositoryCache.FileKey.lenient(File(path), FS.DETECTED)
        return measureTime(shortPath, description) {
            RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build().let(block)
        }
    }

    private inline fun <T> Repository.openGit(description: String = "", block: (JGit) -> T) = open(description) { JGit(it).let(block) }

    private fun JGitRepository.revWalk() = RevWalk(this)

    private fun JGit.revWalk() = RevWalk(repository)

}
