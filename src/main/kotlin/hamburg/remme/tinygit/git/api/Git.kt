package hamburg.remme.tinygit.git.api

import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.LocalStatus
import hamburg.remme.tinygit.printError
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.DiffConfig
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME
import org.eclipse.jgit.lib.Constants.HEAD
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.revwalk.FollowFilter
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.revwalk.RevWalkUtils
import org.eclipse.jgit.revwalk.filter.RevFilter
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import org.eclipse.jgit.treewalk.filter.TreeFilter
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
    private var proxyHost = ThreadLocal<String>() // TODO: really needed? cannot interact with more than one repo atm
    private var proxyPort = ThreadLocal<Int>() // TODO: really needed? cannot interact with more than one repo atm

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

    /**
     * - git remote
     */
    fun hasRemote(repository: LocalRepository): Boolean {
        return repository.openGit("has remote") { it.remoteList().call().isNotEmpty() }
    }

    /**
     * - git remote show origin
     */
    fun getRemote(repository: LocalRepository): String {
        return repository.openGit("get remote") {
            it.remoteList().call()
                    .takeIf { it.isNotEmpty() }?.first()
                    ?.urIs
                    ?.takeIf { it.isNotEmpty() }?.first()
                    ?.toString() ?: ""
        }
    }

    /**
     * - git remote set-url origin <[url]>
     */
    fun setRemote(repository: LocalRepository, url: String) {
        repository.openGit("set remote $url") {
            if (it.remoteList().call().isNotEmpty()) {
                val cmd = it.remoteSetUrl()
                cmd.setName(DEFAULT_REMOTE_NAME)
                cmd.setUri(URIish(url))
                cmd.call()
                cmd.setPush(true)
                cmd.call()
            } else {
                val cmd = it.remoteAdd()
                cmd.setName(DEFAULT_REMOTE_NAME)
                cmd.setUri(URIish(url))
                cmd.call()
            }
        }
    }

    /**
     * TODO
     */
    fun head(repository: LocalRepository): String {
        return repository.open("head") { it.branch }
    }

    /**
     * - git fetch
     * - git branch --all
     * - git log --all --max-count=[max]
     */
    fun log(repository: LocalRepository, fetch: Boolean = false, max: Int = 50): List<LocalCommit> {
        return repository.openGit("log fetch=$fetch") {
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
        return repository.open("head message") {
            val head = it.findRef(it.branch)
            it.revWalk().use { it.parseCommit(head.objectId) }.fullMessage
        }
    }

    /**
     * - git branch --all
     */
    fun branchListAll(repository: LocalRepository): List<LocalBranch> {
        return repository.openGit("branch list") { it.branchListAll() }
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
        return repository.open("status") {
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
                DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED, false)
                DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.COPIED, false) // TODO: cannot happen?
                DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED, false) // TODO: cannot happen?
                DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED, false)
                DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED, false)
            }
        }.sortedBy { it.status }
    }

    private fun DiffFormatter.stagedFiles(repository: Repository): List<LocalFile> {
        val head = repository.resolve(HEAD + "^{tree}")
        val oldTree = if (head != null) repository.newObjectReader().use { CanonicalTreeParser(null, it, head) } else EmptyTreeIterator()
        val newTree = DirCacheIterator(repository.readDirCache())
        return scan(oldTree, newTree).map {
            when (it.changeType!!) {
                DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED)
                DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.COPIED)
                DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED)
                DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED)
            }
        }.sortedBy { it.status }
    }

    /**
     * - git status
     */
    fun divergence(repository: LocalRepository): LocalDivergence {
        return repository.open("divergence") {
            val localBranch = it.findRef(it.branch)?.objectId
            val remoteBranch = it.findRef("$DEFAULT_REMOTE_NAME/${it.branch}")?.objectId
            when {
                localBranch == null -> // TODO: what to do, when there is no branch checked out?
                    LocalDivergence(0, 0)
                remoteBranch == null -> // TODO: count commits since branching?
                    LocalDivergence(-1, 0)
                else -> it.revWalk().use {
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
     * Creates a source code difference using `git diff` depending on the [file]'s status.
     */
    fun diff(repository: LocalRepository, file: LocalFile, lines: Int = 3): String {
        return repository.openGit("diff cached=${file.cached} $file") {
            val diffCommand = it.diff()
                    .setCached(file.cached)
                    .setContextLines(lines)
                    .setPathFilter(file.toPathFilter())
            ByteArrayOutputStream().use {
                diffCommand.setOutputStream(it).call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - git diff --unified=<[lines]> <[commit]> <parent> <[file]>
     */
    fun diff(repository: LocalRepository, file: LocalFile, commit: LocalCommit, lines: Int = 3): String {
        return repository.openGit("diff $file of $commit") {
            val (newTree, oldTree) = it.repository.treesOf(ObjectId.fromString(commit.id))
            val diffCommand = it.diff()
                    .setOldTree(oldTree)
                    .setNewTree(newTree)
                    .setContextLines(lines)
                    .setPathFilter(file.toPathFilter())
            ByteArrayOutputStream().use {
                diffCommand.setOutputStream(it).call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - git diff-tree -r <[commit]>
     */
    fun diffTree(repository: LocalRepository, commit: LocalCommit): List<LocalFile> {
        return repository.open("diff tree $commit") {
            val formatter = DiffFormatter(NullOutputStream.INSTANCE)
            formatter.setRepository(it)
            formatter.isDetectRenames = true

            val (newTree, oldTree) = it.treesOf(ObjectId.fromString(commit.id))
            formatter.scan(oldTree, newTree).map {
                when (it.changeType!!) {
                    DiffEntry.ChangeType.ADD -> LocalFile(it.newPath, LocalFile.Status.ADDED)
                    DiffEntry.ChangeType.COPY -> LocalFile(it.newPath, LocalFile.Status.COPIED)
                    DiffEntry.ChangeType.RENAME -> LocalFile(it.newPath, LocalFile.Status.RENAMED)
                    DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED)
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
        repository.openGit("stage $files") { git ->
            files.filter { it.status == LocalFile.Status.REMOVED }.let { if (it.isNotEmpty()) git.remove(it) }
            files.filter { it.status != LocalFile.Status.REMOVED }.let { if (it.isNotEmpty()) git.add(it) }
        }
    }

    /**
     * - git rm <[removed]>
     * - git add .
     */
    fun stageAll(repository: LocalRepository, removed: List<LocalFile>) {
        repository.openGit("stage all") {
            if (removed.isNotEmpty()) it.remove(removed)
            it.addAll()
        }
    }

    /**
     * - git add --update .
     */
    fun updateAll(repository: LocalRepository) {
        repository.openGit("update") { it.add().setUpdate(true).addFilepattern(".").call() }
    }

    private fun JGit.add(files: List<LocalFile>) {
        val addCommand = add()
        files.forEach { addCommand.addFilepattern(it.path) }
        addCommand.call()
    }

    private fun JGit.addAll() {
        add().addFilepattern(".").call()
    }

    private fun JGit.remove(files: List<LocalFile>) {
        val rmCommand = rm()
        files.forEach { rmCommand.addFilepattern(it.path) }
        rmCommand.call()
    }

    /**
     * - git reset
     */
    fun reset(repository: LocalRepository) {
        repository.openGit("reset") { it.reset().call() }
    }

    /**
     * - git reset <[files]>
     */
    fun reset(repository: LocalRepository, files: List<LocalFile>) {
        repository.openGit("reset $files") {
            val resetCommand = it.reset()
            files.forEach { resetCommand.addPath(it.path) }
            resetCommand.call()
        }
    }

    /**
     * - git reset --hard origin/<current_branch>
     */
    fun resetHard(repository: LocalRepository) {
        repository.openGit("reset hard") {
            it.reset().setMode(ResetCommand.ResetType.HARD).setRef("$DEFAULT_REMOTE_NAME/${it.repository.branch}").call()
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
        repository.openGit("commit amend=$amend") { it.commit().setMessage(message).setAmend(amend).call() }
    }

    /**
     * - git pull
     *
     * @return true if something changed
     */
    fun pull(repository: LocalRepository): Boolean {
        return repository.openGit("pull") {
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
        repository.openGit("push force=$force") {
            val result = it.push().applyAuth(repository).setForce(force).call()
            if (result.count() > 0) {
                result.first().remoteUpdates.forEach {
                    when (it.status) {
                        RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD -> throw PushRejectedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_NODELETE -> throw DeleteRejectedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_REMOTE_CHANGED -> throw RemoteChangedException(it.message)
                        RemoteRefUpdate.Status.REJECTED_OTHER_REASON -> throw RejectedException(it.message)
                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * - git checkout -b [name]
     */
    fun branchCreate(repository: LocalRepository, name: String) {
        repository.openGit("create $name") { it.checkout().setCreateBranch(true).setName(name).call() }
    }

    /**
     * - git branch --move [oldName] [newName]
     */
    fun branchRename(repository: LocalRepository, oldName: String, newName: String) {
        repository.openGit("rename $oldName -> $newName") { it.branchRename().setOldName(oldName).setNewName(newName).call() }
    }

    /**
     * - git branch --delete [name]
     */
    fun branchDelete(repository: LocalRepository, name: String) {
        branchDelete(repository, name, false)
    }

    /**
     * - git branch --delete --force [name]
     */
    fun branchDeleteForce(repository: LocalRepository, name: String) {
        branchDelete(repository, name, true)
    }

    private fun branchDelete(repository: LocalRepository, name: String, force: Boolean) {
        repository.openGit("delete force=$force $name") {
            it.branchDelete().setForce(force).setBranchNames(name).call()
        }
    }

    /**
     * - git checkout [branch]
     */
    fun checkout(repository: LocalRepository, branch: String) {
        repository.openGit("checkout $branch") { it.checkout().setName(branch).call() }
    }

    /**
     * - git checkout <[files]>
     */
    fun checkout(repository: LocalRepository, files: List<LocalFile>) {
        repository.openGit("checkout $files") {
            val checkoutCommand = it.checkout()
            files.forEach { checkoutCommand.addPath(it.path) }
            checkoutCommand.call()
        }
    }

    /**
     * - git checkout -b [local] [remote]
     */
    fun checkoutRemote(repository: LocalRepository, remote: String, local: String? = null) {
        repository.openGit("checkout $remote") {
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
        repository.openGit("stash") { it.stashCreate().call() }
    }

    /**
     * - git stash pop
     *
     * Will not pop on stash apply conflicts/errors
     */
    fun stashPop(repository: LocalRepository) {
        repository.openGit("stash pop") {
            it.stashApply().call()
            it.stashDrop().call()
        }
    }

    /**
     * - git stash list
     */
    fun stashList(repository: LocalRepository): List<LocalStashEntry> {
        return repository.openGit("stash list") { it.stashList().call().map { LocalStashEntry(it.id.name, it.fullMessage) } }
    }

    /**
     * - git stash list
     */
    fun stashListSize(repository: LocalRepository): Int {
        return repository.openGit("stash list size") { it.stashList().call().size }
    }

    /**
     * - git fetch --prune
     * - git gc --aggressive
     */
    fun fetchPrune(repository: LocalRepository) {
        repository.openGit("fetch") {
            it.fetch().applyAuth(repository).setRemoveDeletedRefs(true).call()
            // TODO: clarify when to do
            try {
                it.gc().setAggressive(true).call()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        updatedRepositories += repository
    }

    /**
     * - git init
     */
    fun init(path: File): LocalRepository {
        JGit.init().setDirectory(path).call()
        return LocalRepository(path.absolutePath)
    }

    /**
     * - git clone <[url]>
     */
    fun clone(url: String, path: File): LocalRepository {
        JGit.cloneRepository().setDirectory(path).setURI(url).call()
        return LocalRepository(path.absolutePath)
    }

    private fun JGit.fetch(repository: LocalRepository) {
        if (!isUpdated(repository)) {
            fetch().applyAuth(repository).call()
            updatedRepositories += repository
        }
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: LocalRepository): C {
        val credentials = GitCredentials(repository.ssh, repository.username, repository.password)
        return if (credentials.isSSH) setTransportConfigCallback(credentials.sshTransport)
        else setCredentialsProvider(credentials.userCredentials)
    }

    private inline fun <T> LocalRepository.open(description: String, block: (Repository) -> T): T {
        Git.proxyHost.set(proxyHost)
        Git.proxyPort.set(proxyPort)
        val startTime = System.currentTimeMillis()
        val key = RepositoryCache.FileKey.lenient(File(path), FS.DETECTED)
        val value = RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build().let(block)
        val time = (System.currentTimeMillis() - startTime) / 1000.0
        if (time < 1) println(String.format("[%-18s] %-15s in %6.3fs", this.shortPath, description, time))
        if (time >= 1) printError(String.format("[%-18s] %-15s in %6.3fs", this.shortPath, description, time))
        return value
    }

    private inline fun <T> LocalRepository.openGit(description: String, block: (JGit) -> T) = open(description) { JGit(it).let(block) }

    private fun Repository.revWalk() = RevWalk(this)

    private fun JGit.revWalk() = RevWalk(repository)

    // TODO: test performance if objectreader is created here instead
    private fun Repository.treesOf(commitId: AnyObjectId): Pair<AbstractTreeIterator, AbstractTreeIterator> {
        return revWalk().use { walk ->
            val commit = walk.parseCommit(commitId).takeIf { it.parentCount < 2 }
            val parent = commit?.takeIf { it.parents.isNotEmpty() }?.let { walk.parseCommit(it.parents[0]) }
            walk.iteratorOf(commit) to walk.iteratorOf(parent)
        }
    }

    // TODO: test performance if objectreader is created here instead
    private fun RevWalk.iteratorOf(commit: RevCommit?): AbstractTreeIterator {
        return commit?.let { CanonicalTreeParser(null, objectReader, it.tree) } ?: EmptyTreeIterator()
    }

    private fun LocalFile.toPathFilter(): TreeFilter {
        return if (status == LocalFile.Status.RENAMED || status == LocalFile.Status.COPIED) {
            val config = Config()
            config.setBoolean("diff", null, "renames", true)
            FollowFilter.create(this.path, config.get(DiffConfig.KEY))
        } else {
            PathFilter.create(this.path)
        }
    }

}
