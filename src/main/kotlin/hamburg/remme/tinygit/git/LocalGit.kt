package hamburg.remme.tinygit.git

import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.time.LocalDateTime
import java.time.ZoneOffset

object LocalGit {

    private val updatedRepositories = mutableSetOf<LocalRepository>()
    private var proxyHost = ThreadLocal<String>()
    private var proxyPort = ThreadLocal<Int>()

    init {
        ProxySelector.setDefault(object : ProxySelector() {
            private val delegate = ProxySelector.getDefault()

            override fun select(uri: URI): List<Proxy> {
                return if (proxyHost.get().isNotBlank())
                    listOf(Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(proxyHost.get(), proxyPort.get())))
                else
                    delegate.select(uri)
            }

            override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) = Unit
        })
    }

    fun isUpdated(repository: LocalRepository) = updatedRepositories.contains(repository)

    private fun updated(repository: LocalRepository) = updatedRepositories.add(repository)

    /**
     * - `git remote show origin`
     */
    fun url(repository: LocalRepository): String {
        return repository.open {
            git().remoteList().call()
                    .takeIf { it.isNotEmpty() }?.first()
                    ?.urIs
                    ?.takeIf { it.isNotEmpty() }?.first()
                    ?.toString()
                    ?: ""
        }
    }

    /**
     * TODO
     */
    fun head(repository: LocalRepository): String {
        return repository.open { branch }
    }

    /**
     * - `git fetch`
     * - `git branch --all`
     * - `git log --all --max-count=[max]`
     */
    fun log(repository: LocalRepository, fetch: Boolean = false, max: Int = 50): List<LocalCommit> {
        return repository.open {
            val git = git()
            if (fetch) git.fetch(repository)
            val log = git.log()
            val branches = git.branchListAll()
            branches.forEach { log.add(ObjectId.fromString(it.commit)) }
            log.setMaxCount(max).call().map { c ->
                LocalCommit(
                        c.id.name, c.abbreviate(10).name(),
                        c.parents.map { it.abbreviate(10).name() },
                        c.fullMessage, c.shortMessage,
                        c.commitTime(),
                        c.authorIdent.name,
                        c.authorIdent.emailAddress,
                        branches.filter { it.commit == c.id.name })
            }
        }
    }

    private fun RevCommit.commitTime()
            = LocalDateTime.ofEpochSecond(commitTime.toLong(), 0,
            ZoneOffset.ofTotalSeconds(authorIdent.timeZoneOffset * 60))

    /**
     * - `git log HEAD --max-count=1`
     */
    fun headMessage(repository: LocalRepository): String {
        return repository.open {
            val head = findRef(branch)
            revWalk().use { it.parseCommit(head.objectId) }.fullMessage
        }
    }

    /**
     * - `git branch --all`
     */
    fun branchListAll(repository: LocalRepository): List<LocalBranch> {
        return repository.open { git().branchListAll() }
    }

    private fun Git.branchListAll(): List<LocalBranch> {
        return RevWalk(repository).use { walker ->
            branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map {
                val shortRef = Repository.shortenRefName(it.name)
                LocalBranch(
                        shortRef,
                        walker.lookupCommit(it.objectId).id.name,
                        it.name.contains("remotes"))
            }
        }
    }

    /**
     * - `git status`
     */
    fun status(repository: LocalRepository): LocalStatus {
        return repository.open {
            val status = git().status().call()

            val staged = mutableListOf<LocalFile>()
            staged += status.conflicting.toLocalFileList(LocalFile.Status.CONFLICT)
            staged += status.added.toLocalFileList(LocalFile.Status.ADDED)
            staged += status.changed.toLocalFileList(LocalFile.Status.CHANGED)
            staged += status.removed.toLocalFileList(LocalFile.Status.REMOVED)

            val pending = mutableListOf<LocalFile>()
            pending += status.conflicting.toLocalFileList(LocalFile.Status.CONFLICT)
            pending += status.modified.toLocalFileList(LocalFile.Status.MODIFIED)
            pending += status.missing.toLocalFileList(LocalFile.Status.MISSING)
            pending += status.untracked.toLocalFileList(LocalFile.Status.UNTRACKED)

            LocalStatus(staged, pending)
        }
    }

    private fun Set<String>.toLocalFileList(status: LocalFile.Status): List<LocalFile> {
        return map { LocalFile(it, status) }.sortedBy { it.path }
    }

    /**
     * TODO
     */
    fun divergence(repository: LocalRepository, local: String? = null, remote: String? = null): LocalDivergence {
        return repository.open {
            val localBranch = findRef(local ?: branch).objectId
            val remoteBranch = findRef(remote ?: "${Constants.DEFAULT_REMOTE_NAME}/$branch")?.objectId
            if (remoteBranch == null) {
                // TODO: count commits since branching?
                LocalDivergence(-1, 0)
            } else {
                revWalk().use {
                    val localCommit = it.parseCommit(localBranch)
                    val remoteCommit = it.parseCommit(remoteBranch)
                    it.revFilter = RevFilter.MERGE_BASE
                    it.markStart(localCommit)
                    it.markStart(remoteCommit)
                    val mergeBase = it.next()
                    it.reset()
                    it.revFilter = RevFilter.ALL
                    LocalDivergence(
                            RevWalkUtils.count(it, localCommit, mergeBase),
                            RevWalkUtils.count(it, remoteCommit, mergeBase))
                }
            }
        }
    }

    /**
     * - `git diff <[file]>`
     */
    fun diff(repository: LocalRepository, file: LocalFile): String {
        return diff(repository, file, false)
    }

    /**
     * - `git diff --cached <[file]>`
     */
    fun diffCached(repository: LocalRepository, file: LocalFile): String {
        return diff(repository, file, true)
    }

    private fun diff(repository: LocalRepository, file: LocalFile, cached: Boolean, lines: Int = -1): String {
        return repository.open {
            ByteArrayOutputStream().use {
                git().diff()
                        .setCached(cached)
                        .setPathFilter(PathFilter.create(file.path))
                        .setContextLines(lines)
                        .setOutputStream(it)
                        .call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - `git diff <[id]> <parent-id> <[file]>`
     */
    fun diff(repository: LocalRepository, file: LocalFile, id: String): String {
        return repository.open {
            val (newTree, oldTree) = newObjectReader().treesOf(ObjectId.fromString(id))
            ByteArrayOutputStream().use {
                git().diff().setPathFilter(PathFilter.create(file.path)).setNewTree(newTree).setOldTree(oldTree).setOutputStream(it).call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - `git diff-tree -r <[id]>`
     */
    fun diffTree(repository: LocalRepository, id: String): List<LocalFile> {
        return repository.open {
            val (newTree, oldTree) = newObjectReader().treesOf(ObjectId.fromString(id))
            git().diff().setNewTree(newTree).setOldTree(oldTree).call().map {
                when (it.changeType!!) {
                    DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED)
                    DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.ADDED) // TODO: status for copied files
                    DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED)
                    DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.MODIFIED) // TODO: status for renamed files
                    DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED)
                }
            }.sortedBy { it.status }
        }
    }

    private fun ObjectReader.treesOf(commitId: AnyObjectId): Pair<AbstractTreeIterator, AbstractTreeIterator> {
        return RevWalk(this).use { walker ->
            val commit = walker.parseCommit(commitId)
            val parent = commit.parents.takeIf { it.isNotEmpty() }?.let { walker.parseCommit(it[0]) }
            iteratorOf(commit) to iteratorOf(parent)
        }
    }

    private fun ObjectReader.iteratorOf(commit: RevCommit?): AbstractTreeIterator {
        return commit?.let { CanonicalTreeParser(null, this, it.tree) } ?: EmptyTreeIterator()
    }

    /**
     * - `git add <[files]>`
     */
    fun add(repository: LocalRepository, files: List<LocalFile>) {
        return repository.open {
            val git = git().add()
            files.forEach { git.addFilepattern(it.path) }
            git.call()
        }
    }

    /**
     * - `git add .`
     */
    fun addAll(repository: LocalRepository) {
        return repository.open { git().add().addFilepattern(".").call() }
    }

    /**
     * - `git add --update <[files]>`
     */
    fun addUpdate(repository: LocalRepository, files: List<LocalFile>) {
        return repository.open {
            val git = git().add().setUpdate(true)
            files.forEach { git.addFilepattern(it.path) }
            git.call()
        }
    }

    /**
     * - `git add --update .`
     */
    fun addAllUpdate(repository: LocalRepository) {
        return repository.open { git().add().setUpdate(true).addFilepattern(".").call() }
    }

    /**
     * - `git reset`
     */
    fun reset(repository: LocalRepository) {
        return repository.open { git().reset().call() }
    }

    /**
     * - `git reset <[files]>`
     */
    fun reset(repository: LocalRepository, files: List<LocalFile>) {
        return repository.open {
            val git = git().reset()
            files.forEach { git.addPath(it.path) }
            git.call()
        }
    }

    /**
     * - `git commit --message="[message]"`
     */
    fun commit(repository: LocalRepository, message: String) {
        commit(repository, message, false)
    }

    /**
     * - `git commit --amend --message="[message]"`
     */
    fun commitAmend(repository: LocalRepository, message: String) {
        commit(repository, message, true)
    }

    private fun commit(repository: LocalRepository, message: String, amend: Boolean) {
        repository.open { git().commit().setMessage(message).setAmend(amend).call() }
    }

    /**
     * - `git pull`
     *
     * @return `true` if something changed
     */
    fun pull(repository: LocalRepository): Boolean {
        return repository.open {
            git().pull().applyAuth(repository).call().mergeResult.mergeStatus != MergeResult.MergeStatus.ALREADY_UP_TO_DATE
        }
    }

    /**
     * - `git push`
     */
    fun push(repository: LocalRepository) {
        push(repository, false)
    }

    /**
     * - `git push --force`
     */
    fun pushForce(repository: LocalRepository) {
        push(repository, true)
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        repository.open {
            val result = git().push().applyAuth(repository).setForce(force).call()
            if (result.count() > 0) {
                result.first().remoteUpdates.forEach {
                    when (it.status) {
                        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> throw PushRejectedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_NODELETE -> throw DeleteRejectedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> throw RemoteChangedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> throw RejectedException(it.message)
                        else -> {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

    /**
     * - `git checkout -b [name]`
     */
    fun branchCreate(repository: LocalRepository, name: String) {
        repository.open { git().checkout().setCreateBranch(true).setName(name).call() }
    }

    fun branchDelete(repository: LocalRepository, name: String) {
        repository.open {
            git()
        }
    }

    /**
     * - `git checkout [branch]`
     */
    fun checkout(repository: LocalRepository, branch: String) {
        repository.open { git().checkout().setName(branch).call() }
    }

    /**
     * - `git checkout -b [local] [remote]`
     */
    fun checkoutRemote(repository: LocalRepository, remote: String, local: String? = null) {
        repository.open {
            git().checkout()
                    .setCreateBranch(true)
                    .setName(local ?: remote.substringAfter('/'))
                    .setStartPoint(remote)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call()
        }
    }

    /**
     * - `git stash`
     */
    fun stash(repository: LocalRepository) {
        repository.open { git().stashCreate().call() }
    }

    /**
     * - `git stash pop`
     *
     * Will not pop on stash apply conflicts/errors
     */
    fun stashPop(repository: LocalRepository) {
        repository.open {
            val git = git()
            git.stashApply().call()
            git.stashDrop().call()
        }
    }

    /**
     * - `git stash list`
     */
    fun stashList(repository: LocalRepository): List<LocalStashEntry> {
        return repository.open { git().stashList().call().map { LocalStashEntry(it.id.name, it.fullMessage) } }
    }

    /**
     * - `git stash list`
     */
    fun stashListSize(repository: LocalRepository): Int {
        return repository.open { git().stashList().call().size }
    }

    /**
     * - `git fetch --prune`
     */
    fun fetchPrune(repository: LocalRepository) {
        repository.open { git().fetch().applyAuth(repository).setRemoveDeletedRefs(true).call() }
        updated(repository)
    }

    private fun Git.fetch(repository: LocalRepository) {
        if (!isUpdated(repository)) {
            fetch().applyAuth(repository).call()
            updated(repository)
        }
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: LocalRepository): C {
        return if (repository.credentials?.isSSH() == true)
            setTransportConfigCallback(repository.credentials!!.sshTransport)
        else
            setCredentialsProvider(repository.credentials?.userCredentials)
    }

    private fun <T> LocalRepository.open(block: Repository.() -> T): T {
        this@LocalGit.proxyHost.set(proxyHost ?: "")
        this@LocalGit.proxyPort.set(proxyPort ?: 80)
        return FileRepositoryBuilder().setGitDir(File("$path/.git")).build().use(block)
    }

    private fun Repository.git() = Git(this)

    private fun Repository.revWalk() = RevWalk(this)

}
