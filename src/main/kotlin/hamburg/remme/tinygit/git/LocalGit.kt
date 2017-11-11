package hamburg.remme.tinygit.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
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
     * - `git fetch`
     * - `git branch --all`
     * - `git log --all --max-count=[max]`
     */
    fun log(repository: LocalRepository, fetch: Boolean = false, max: Int = 50): List<LocalCommit> {
        return repository.open {
            val git = Git(it)
            if (fetch) git.fetch(repository)
            val branches = git.branchListAll()
            git.log().all().setMaxCount(max).call().map { c ->
                LocalCommit(
                        c.id.name, c.abbreviate(10).name(),
                        c.parents.map { it.abbreviate(10).name() },
                        c.fullMessage, c.shortMessage,
                        c.commitTime(),
                        c.author(),
                        branches.filter { it.commit == c.id.name })
            }
        }
    }

    private fun RevCommit.commitTime()
            = LocalDateTime.ofEpochSecond(this.commitTime.toLong(), 0,
            ZoneOffset.ofTotalSeconds(this.authorIdent.timeZoneOffset * 60))

    private fun RevCommit.author() = "${this.authorIdent.name} <${this.authorIdent.emailAddress}>"

    /**
     * - `git branch --all`
     */
    fun branchListAll(repository: LocalRepository): List<LocalBranch> {
        return repository.open { Git(it).branchListAll() }
    }

    private fun Git.branchListAll(): List<LocalBranch> {
        return RevWalk(this.repository).use { walker ->
            this.branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map {
                val shortRef = Repository.shortenRefName(it.name)
                LocalBranch(
                        shortRef,
                        walker.lookupCommit(it.objectId).id.name,
                        shortRef == this.repository.branch,
                        it.name.contains("remotes"))
            }
        }
    }

    /**
     * - `git status`
     */
    fun status(repository: LocalRepository): LocalStatus {
        return repository.open {
            val status = Git(it).status().call()

            val staged = mutableListOf<LocalFile>()
            staged += status.conflicting.toLocalFileList(LocalFile.Status.CONFLICT)
            staged += status.added.toLocalFileList(LocalFile.Status.ADDED)
            staged += status.changed.toLocalFileList(LocalFile.Status.CHANGED)
            staged += status.removed.toLocalFileList(LocalFile.Status.REMOVED)

            val unstaged = mutableListOf<LocalFile>()
            unstaged += status.conflicting.toLocalFileList(LocalFile.Status.CONFLICT)
            unstaged += status.modified.toLocalFileList(LocalFile.Status.MODIFIED)
            unstaged += status.missing.toLocalFileList(LocalFile.Status.MISSING)
            unstaged += status.untracked.toLocalFileList(LocalFile.Status.UNTRACKED)

            LocalStatus(staged, unstaged)
        }
    }

    private fun Set<String>.toLocalFileList(status: LocalFile.Status): List<LocalFile> {
        return this.map { LocalFile(it, status) }.sortedBy { it.path }
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
        return repository.open { gitRepo ->
            ByteArrayOutputStream().use {
                Git(gitRepo).diff()
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
        return repository.open { gitRepo ->
            val (newTree, oldTree) = gitRepo.newObjectReader().treesOf(ObjectId.fromString(id))
            ByteArrayOutputStream().use {
                Git(gitRepo).diff().setPathFilter(PathFilter.create(file.path)).setNewTree(newTree).setOldTree(oldTree).setOutputStream(it).call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - `git diff-tree -r <[id]>`
     */
    fun diffTree(repository: LocalRepository, id: String): List<LocalFile> {
        return repository.open { gitRepo ->
            val (newTree, oldTree) = gitRepo.newObjectReader().treesOf(ObjectId.fromString(id))
            Git(gitRepo).diff().setNewTree(newTree).setOldTree(oldTree).call().map {
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
            this.iteratorOf(commit) to this.iteratorOf(parent)
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
            val git = Git(it).add()
            files.forEach { git.addFilepattern(it.path) }
            git.call()
        }
    }

    /**
     * - `git add .`
     */
    fun addAll(repository: LocalRepository) {
        return repository.open { Git(it).add().addFilepattern(".").call() }
    }

    /**
     * - `git add --update <[files]>`
     */
    fun addUpdate(repository: LocalRepository, files: List<LocalFile>) {
        return repository.open {
            val git = Git(it).add().setUpdate(true)
            files.forEach { git.addFilepattern(it.path) }
            git.call()
        }
    }

    /**
     * - `git add --update .`
     */
    fun addAllUpdate(repository: LocalRepository) {
        return repository.open { Git(it).add().setUpdate(true).addFilepattern(".").call() }
    }

    /**
     * - `git reset`
     */
    fun reset(repository: LocalRepository) {
        return repository.open { Git(it).reset().call() }
    }

    /**
     * - `git reset <[files]>`
     */
    fun reset(repository: LocalRepository, files: List<LocalFile>) {
        return repository.open {
            val git = Git(it).reset()
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
        repository.open { Git(it).commit().setMessage(message).setAmend(amend).call() }
    }

    /**
     * - `git pull`
     *
     * @return `true` if something changed
     */
    fun pull(repository: LocalRepository): Boolean {
        return repository.open {
            Git(it).pull().applyAuth(repository).call().mergeResult.mergeStatus != MergeResult.MergeStatus.ALREADY_UP_TO_DATE
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

    // TODO: return boolean if something changed
    private fun push(repository: LocalRepository, force: Boolean) {
        repository.open {
            val result = Git(it).push().applyAuth(repository).setForce(force).call()
            if (result.count() > 0) {
                result.first().remoteUpdates.forEach {
                    if (it.status == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                        throw PushRejectedException()
                    }
                }
            }
        }
    }

    /**
     * - `git checkout -b [name]`
     */
    fun branchCreate(repository: LocalRepository, name: String) {
        repository.open { Git(it).checkout().setCreateBranch(true).setName(name).call() }
    }

    /**
     * - `git checkout [name]`
     */
    fun checkout(repository: LocalRepository, name: String) {
        repository.open { Git(it).checkout().setName(name).call() }
    }

    /**
     * - `git stash`
     */
    fun stash(repository: LocalRepository) {
        repository.open { Git(it).stashCreate().call() }
    }

    /**
     * - `git stash pop`
     */
    fun stashPop(repository: LocalRepository) {
        repository.open {
            val git = Git(it)
            git.stashApply().call()
            git.stashDrop().call()
        }
    }

    /**
     * - `git stash list`
     */
    fun stashList(repository: LocalRepository): Int {
        return repository.open { Git(it).stashList().call().size }
    }

    /**
     * - `git fetch --prune`
     */
    fun fetchPrune(repository: LocalRepository) {
        repository.open { Git(it).fetch().applyAuth(repository).setRemoveDeletedRefs(true).call() }
        updated(repository)
    }

    private fun Git.fetch(repository: LocalRepository) {
        if (!isUpdated(repository)) {
            this.fetch().applyAuth(repository).call()
            updated(repository)
        }
    }

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: LocalRepository): C {
        return if (repository.credentials?.isSSH() == true)
            this.setTransportConfigCallback(repository.credentials!!.sshTransport)
        else
            this.setCredentialsProvider(repository.credentials?.userCredentials)
    }

    private fun <T> LocalRepository.open(block: (Repository) -> T): T {
        this@LocalGit.proxyHost.set(proxyHost ?: "")
        this@LocalGit.proxyPort.set(proxyPort ?: 80)
        return FileRepositoryBuilder().setGitDir(File("${this.path}/.git")).build().use(block)
    }

}
