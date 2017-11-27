package hamburg.remme.tinygit.git.api

import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.LocalStatus
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME
import org.eclipse.jgit.lib.Constants.HEAD
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.util.FS
import org.eclipse.jgit.util.io.NullOutputStream
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
import org.eclipse.jgit.api.Git as JGit

object Git {

    private val updatedRepositories = mutableSetOf<LocalRepository>()
    private val repositoryCache = mutableMapOf<String, Repository>()
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

    fun isOpen(key: String) = repositoryCache.contains(key)

    /**
     * - git remote show origin
     */
    fun url(repository: LocalRepository): String {
        return repository.openGit {
            it.remoteList().call()
                    .takeIf { it.isNotEmpty() }?.first()
                    ?.urIs
                    ?.takeIf { it.isNotEmpty() }?.first()
                    ?.toString() ?: ""
        }
    }

    /**
     * TODO
     */
    fun head(repository: LocalRepository): String {
        return repository.open { it.branch }
    }

    /**
     * - git fetch
     * - git branch --all
     * - git log --all --max-count=[max]
     */
    fun log(repository: LocalRepository, fetch: Boolean = false, max: Int = 50): List<LocalCommit> {
        return repository.openGit {
            if (fetch) it.fetch(repository)
            val branches = it.branchListAll()
            val logCommand = it.log()
            branches.forEach { logCommand.add(ObjectId.fromString(it.commit)) }
            logCommand.setMaxCount(max).call().map { c ->
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
     * - git log HEAD --max-count=1
     */
    fun headMessage(repository: LocalRepository): String {
        return repository.open {
            val head = it.findRef(it.branch)
            it.revWalk().use { it.parseCommit(head.objectId) }.fullMessage
        }
    }

    /**
     * - git branch --all
     */
    fun branchListAll(repository: LocalRepository): List<LocalBranch> {
        return repository.openGit { it.branchListAll() }
    }

    private fun JGit.branchListAll(): List<LocalBranch> {
        return revWalk().use { walk ->
            branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map {
                val shortRef = Repository.shortenRefName(it.name)
                LocalBranch(
                        shortRef,
                        walk.lookupCommit(it.objectId).id.name,
                        it.name.contains("remotes"))
            }
        }
    }

    /**
     * - git status
     */
    fun status(repository: LocalRepository): LocalStatus {
        return repository.open {
            val formatter = DiffFormatter(NullOutputStream.INSTANCE)
            formatter.setRepository(it)
            formatter.isDetectRenames = true
            LocalStatus(formatter.stagedFiles(it), formatter.pendingFiles(it))
        }
    }

    private fun DiffFormatter.pendingFiles(repository: Repository): List<LocalFile> {
        val oldTree = DirCacheIterator(repository.readDirCache())
        val newTree = FileTreeIterator(repository)
        return scan(oldTree, newTree).map {
            when (it.changeType!!) {
                DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.UNTRACKED)
                DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.RENAMED) // TODO: cannot happen?
                DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED) // TODO: cannot happen?
                DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED)
                DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.MISSING)
            }
        }
    }

    private fun DiffFormatter.stagedFiles(repository: Repository): List<LocalFile> {
        val head = repository.resolve(HEAD + "^{tree}")
        val oldTree = if (head != null) repository.newObjectReader().use { CanonicalTreeParser(null, it, head) } else EmptyTreeIterator()
        val newTree = DirCacheIterator(repository.readDirCache())
        return scan(oldTree, newTree).map {
            when (it.changeType!!) {
                DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED)
                DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.CHANGED)
                DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED)
            }
        }
    }

    /**
     * TODO
     */
    fun divergence(repository: LocalRepository, local: String? = null, remote: String? = null): LocalDivergence {
        return repository.open {
            val localBranch = it.findRef(local ?: it.branch).objectId
            val remoteBranch = it.findRef(remote ?: "$DEFAULT_REMOTE_NAME/${it.branch}")?.objectId
            if (remoteBranch == null) {
                // TODO: count commits since branching?
                LocalDivergence(-1, 0)
            } else {
                it.revWalk().use {
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
     * - git diff <[file]>
     */
    fun diff(repository: LocalRepository, file: LocalFile): String {
        return diff(repository, file, false)
    }

    /**
     * - git diff --cached <[file]>
     */
    fun diffCached(repository: LocalRepository, file: LocalFile): String {
        return diff(repository, file, true)
    }

    /**
     * - git diff --unified=<[lines]> --[cached] <[file]>
     */
    // TODO: does not work with renames
    private fun diff(repository: LocalRepository, file: LocalFile, cached: Boolean, lines: Int = 3): String {
        return repository.open { gitRepo ->
            ByteArrayOutputStream().use {
                val formatter = DiffFormatter(it)
                formatter.setRepository(gitRepo)
                formatter.setContext(lines)
                formatter.isDetectRenames = true
                formatter.pathFilter = PathFilter.create(file.path)

                if (cached) formatter.stagedDiff(gitRepo) else formatter.pendingDiff(gitRepo)

                it.toString("UTF-8")
            }
        }
    }

    // TODO: does not work with renames
    private fun DiffFormatter.pendingDiff(repository: Repository) {
        val oldTree = DirCacheIterator(repository.readDirCache())
        val newTree = FileTreeIterator(repository)
        format(scan(oldTree, newTree))
        flush()
    }

    // TODO: does not work with renames
    private fun DiffFormatter.stagedDiff(repository: Repository) {
        val head = repository.resolve(HEAD + "^{tree}")
        val oldTree = if (head != null) repository.newObjectReader().use { CanonicalTreeParser(null, it, head) } else EmptyTreeIterator()
        val newTree = DirCacheIterator(repository.readDirCache())
        format(scan(oldTree, newTree))
        flush()
    }

    /**
     * - git diff --unified=<[lines]> <[id]> <parent-id> <[file]>
     */
    // TODO: does not work with renames
    fun diff(repository: LocalRepository, file: LocalFile, id: String, lines: Int = 3): String {
        return repository.open { gitRepo ->
            ByteArrayOutputStream().use {
                val formatter = DiffFormatter(it)
                formatter.setRepository(gitRepo)
                formatter.setContext(lines)
                formatter.isDetectRenames = true
                formatter.pathFilter = PathFilter.create(file.path)

                val (newTree, oldTree) = gitRepo.treesOf(ObjectId.fromString(id))
                formatter.format(formatter.scan(oldTree, newTree))
                formatter.flush()

                it.toString("UTF-8")
            }
        }
    }

    /**
     * - git diff-tree -r <[id]>
     */
    fun diffTree(repository: LocalRepository, id: String): List<LocalFile> {
        return repository.open {
            val formatter = DiffFormatter(NullOutputStream.INSTANCE)
            formatter.setRepository(it)
            formatter.isDetectRenames = true

            val (newTree, oldTree) = it.treesOf(ObjectId.fromString(id))
            formatter.scan(oldTree, newTree).map {
                when (it.changeType!!) {
                    DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED)
                    DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                    DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                    DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.CHANGED)
                    DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED)
                }
            }.sortedBy { it.status }
        }
    }

    /**
     * - git rm <[files] != MISSING>
     * - git add <[files] != MISSING>
     */
    fun stage(repository: LocalRepository, files: List<LocalFile>) {
        repository.openGit { git ->
            files.filter { it.status == LocalFile.Status.MISSING }.let { if (it.isNotEmpty()) git.remove(it) }
            files.filter { it.status != LocalFile.Status.MISSING }.let { if (it.isNotEmpty()) git.add(it) }
        }
    }

    /**
     * - git rm <[removed]>
     * - git add .
     */
    fun stageAll(repository: LocalRepository, removed: List<LocalFile>) {
        repository.openGit {
            if (removed.isNotEmpty()) it.remove(removed)
            it.addAll()
        }
    }

    /**
     * - git add --update .
     */
    fun updateAll(repository: LocalRepository) {
        repository.openGit { it.add().setUpdate(true).addFilepattern(".").call() }
    }

    /**
     * - git add <[files]>
     */
    private fun JGit.add(files: List<LocalFile>) {
        val addCommand = add()
        files.forEach { addCommand.addFilepattern(it.path) }
        addCommand.call()
    }

    /**
     * - git add .
     */
    private fun JGit.addAll() {
        add().addFilepattern(".").call()
    }

    /**
     * - git rm <[files]>
     */
    private fun JGit.remove(files: List<LocalFile>) {
        val rmCommand = rm()
        files.forEach { rmCommand.addFilepattern(it.path) }
        rmCommand.call()
    }

    /**
     * - git reset
     */
    fun reset(repository: LocalRepository) {
        repository.openGit { it.reset().call() }
    }

    /**
     * - git reset <[files]>
     */
    fun reset(repository: LocalRepository, files: List<LocalFile>) {
        repository.openGit {
            val resetCommand = it.reset()
            files.forEach { resetCommand.addPath(it.path) }
            resetCommand.call()
        }
    }

    /**
     * - git commit --message="[message]"
     */
    fun commit(repository: LocalRepository, message: String) {
        commit(repository, message, false)
    }

    /**
     * - git commit --amend --message="[message]"
     */
    fun commitAmend(repository: LocalRepository, message: String) {
        commit(repository, message, true)
    }

    private fun commit(repository: LocalRepository, message: String, amend: Boolean) {
        repository.openGit { it.commit().setMessage(message).setAmend(amend).call() }
    }

    /**
     * - git pull
     *
     * @return true if something changed
     */
    fun pull(repository: LocalRepository): Boolean {
        return repository.openGit {
            it.pull().applyAuth(repository).call().mergeResult.mergeStatus != MergeResult.MergeStatus.ALREADY_UP_TO_DATE
        }
    }

    /**
     * - git push
     */
    fun push(repository: LocalRepository) {
        push(repository, false)
    }

    /**
     * - git push --force
     */
    fun pushForce(repository: LocalRepository) {
        push(repository, true)
    }

    private fun push(repository: LocalRepository, force: Boolean) {
        repository.openGit {
            val result = it.push().applyAuth(repository).setForce(force).call()
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
     * - git checkout -b [name]
     */
    fun branchCreate(repository: LocalRepository, name: String) {
        repository.openGit { it.checkout().setCreateBranch(true).setName(name).call() }
    }

    /**
     * - git branch --delete [name]
     */
    fun branchDelete(repository: LocalRepository, name: String) {
        repository.openGit { it.branchDelete().setBranchNames(name).call() }
    }

    /**
     * - git checkout [branch]
     */
    fun checkout(repository: LocalRepository, branch: String) {
        repository.openGit { it.checkout().setName(branch).call() }
    }

    /**
     * - git checkout -b [local] [remote]
     */
    fun checkoutRemote(repository: LocalRepository, remote: String, local: String? = null) {
        repository.openGit {
            it.checkout()
                    .setCreateBranch(true)
                    .setName(local ?: remote.substringAfter('/'))
                    .setStartPoint(remote)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call()
        }
    }

    /**
     * - git stash
     */
    fun stash(repository: LocalRepository) {
        repository.openGit { it.stashCreate().call() }
    }

    /**
     * - git stash pop
     *
     * Will not pop on stash apply conflicts/errors
     */
    fun stashPop(repository: LocalRepository) {
        repository.openGit {
            it.stashApply().call()
            it.stashDrop().call()
        }
    }

    /**
     * - git stash list
     */
    fun stashList(repository: LocalRepository): List<LocalStashEntry> {
        return repository.openGit { it.stashList().call().map { LocalStashEntry(it.id.name, it.fullMessage) } }
    }

    /**
     * - git stash list
     */
    fun stashListSize(repository: LocalRepository): Int {
        return repository.openGit { it.stashList().call().size }
    }

    /**
     * - git fetch --prune
     */
    fun fetchPrune(repository: LocalRepository) {
        repository.openGit { it.fetch().applyAuth(repository).setRemoveDeletedRefs(true).call() }
        updatedRepositories += repository
    }

    private fun JGit.fetch(repository: LocalRepository) {
        if (!isUpdated(repository)) {
            fetch().applyAuth(repository).call()
            updatedRepositories += repository
        }
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: LocalRepository): C {
        val credentials = GitCredentials(repository.ssh, repository.username, repository.password)
        return if (credentials.isSSH()) setTransportConfigCallback(credentials.sshTransport)
        else setCredentialsProvider(credentials.userCredentials)
    }

    private inline fun <T> LocalRepository.open(block: (Repository) -> T): T {
        Git.proxyHost.set(proxyHost)
        Git.proxyPort.set(proxyPort)
        return if (isOpen(path)) {
            repositoryCache[path]!!
        } else {
            val key = RepositoryCache.FileKey.lenient(File(path), FS.DETECTED)
            val repository = RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build()
            repositoryCache[path] = repository
            repository
        }.let(block)
    }

    private inline fun <T> LocalRepository.openGit(block: (JGit) -> T) = open { JGit(it).let(block) }

    private fun Repository.revWalk() = RevWalk(this)

    // TODO: test performance if objectreader is created here instead
    private fun Repository.treesOf(commitId: AnyObjectId): Pair<AbstractTreeIterator, AbstractTreeIterator> {
        return revWalk().use { walk ->
            val commit = walk.parseCommit(commitId).takeIf { it.parents.size < 2 }
            val parent = commit?.let { walk.parseCommit(it.parents[0]) }
            walk.iteratorOf(commit) to walk.iteratorOf(parent)
        }
    }

    private fun JGit.revWalk() = RevWalk(repository)

    private fun JGit.treesOf(commitId: AnyObjectId) = repository.treesOf(commitId)

    // TODO: test performance if objectreader is created here instead
    private fun RevWalk.iteratorOf(commit: RevCommit?): AbstractTreeIterator {
        return commit?.let { CanonicalTreeParser(null, objectReader, it.tree) } ?: EmptyTreeIterator()
    }

}
