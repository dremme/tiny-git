package hamburg.remme.tinygit.git.api

import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.git.LocalBranch
import hamburg.remme.tinygit.git.LocalCommit
import hamburg.remme.tinygit.git.LocalDivergence
import hamburg.remme.tinygit.git.LocalFile
import hamburg.remme.tinygit.git.LocalRebase
import hamburg.remme.tinygit.git.LocalRepository
import hamburg.remme.tinygit.git.LocalStashEntry
import hamburg.remme.tinygit.git.LocalStatus
import hamburg.remme.tinygit.readFirst
import hamburg.remme.tinygit.readLines
import hamburg.remme.tinygit.stopTime
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.diff.DiffConfig
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.lib.RepositoryState
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

    private val REMOTE = Constants.DEFAULT_REMOTE_NAME
    private val HEAD = Constants.HEAD
    private val DEFAULT_BRANCHES = arrayOf("master", "develop", "trunk")
    private val cache = mutableSetOf<LocalRepository>()
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

    fun isUpdated(repository: LocalRepository) = cache.contains(repository)

    fun isDefaultBranch(repository: LocalRepository): Boolean {
        return repository.open { DEFAULT_BRANCHES.contains(it.branch) }
    }

    fun isMerging(repository: LocalRepository): Boolean {
        return repository.open { it.repositoryState == RepositoryState.MERGING }
    }

    fun isRebasing(repository: LocalRepository): Boolean {
        return repository.open { it.repositoryState.isRebasing }
    }

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
     * One of the following:
     * - git remote add origin <[url]>
     * - git remote set-url origin <[url]>
     */
    fun setRemote(repository: LocalRepository, url: String) {
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
    fun removeRemote(repository: LocalRepository) {
        repository.openGit("remove remote") {
            val cmd = it.remoteRemove()
            cmd.setName(REMOTE)
            cmd.call()
        }
    }

    fun head(repository: LocalRepository): String {
        return repository.open("head") { it.branch }
    }

    /**
     * - git branch --all
     * - git log --all --skip=[skip] --max-count=[max]
     */
    fun log(repository: LocalRepository, skip: Int, max: Int): List<LocalCommit> {
        return log(repository, skip, max, false)
    }

    /**
     * - git fetch
     * - git branch --all
     * - git log --all --skip=[skip] --max-count=[max]
     */
    fun logFetch(repository: LocalRepository, skip: Int, max: Int): List<LocalCommit> {
        return log(repository, skip, max, true)
    }

    private fun log(repository: LocalRepository, skip: Int, max: Int, fetch: Boolean): List<LocalCommit> {
        return repository.openGit("log $skip->${max + skip}") {
            if (fetch) it.fetch(repository)
            val logCommand = it.log().setSkip(skip).setMaxCount(max)
            it.branchListIds().forEach { logCommand.add(it) }
            logCommand.call().map { it.toLocalCommit() }
        }
    }

    /**
     * - git log master..
     */
    fun logWithoutDefault(repository: LocalRepository): List<LocalCommit> {
        return repository.open("prep squash") {
            val head = it.resolve(it.branch)
            val defaultIds = DEFAULT_BRANCHES.mapNotNull(it::findRef).map { it.objectId }
            it.revWalk().use {
                defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                it.markStart(it.parseCommit(head))
                it.map { it.toLocalCommit() }
            }
        }
    }

    private fun RevCommit.toLocalCommit() = LocalCommit(
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
        return branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map {
            LocalBranch(
                    Repository.shortenRefName(it.name),
                    it.objectId.name,
                    it.name.startsWith(Constants.R_REMOTES))
        }
    }

    private fun JGit.branchListIds(): List<AnyObjectId> {
        return branchList().setListMode(ListBranchCommand.ListMode.ALL).call().map { it.objectId }
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
                DiffEntry.ChangeType.COPY -> throw IllegalStateException("Type not allowed: ${DiffEntry.ChangeType.COPY}")
                DiffEntry.ChangeType.RENAME -> throw IllegalStateException("Type not allowed: ${DiffEntry.ChangeType.RENAME}")
                DiffEntry.ChangeType.MODIFY -> LocalFile(it.newPath, LocalFile.Status.MODIFIED, false)
                DiffEntry.ChangeType.DELETE -> LocalFile(it.oldPath, LocalFile.Status.REMOVED, false)
            }
        }.detectConflicts().sortedBy { it.status }
    }

    private fun DiffFormatter.stagedFiles(repository: Repository): List<LocalFile> {
        val head = repository.resolve("$HEAD^{tree}")
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
        }.detectConflicts().sortedBy { it.status }
    }

    private fun List<LocalFile>.detectConflicts(): List<LocalFile> {
        val conflicts = filter { file -> count { it.path == file.path } > 1 }.toSet()
        return filter { !conflicts.contains(it) } + conflicts.map { LocalFile(it.path, LocalFile.Status.CONFLICT, it.cached) }
    }

    /**
     * - git status
     */
    fun divergence(repository: LocalRepository): LocalDivergence {
        return repository.open("divergence") {
            val localBranch = it.findRef(it.branch)?.objectId
            val remoteBranch = it.findRef("$REMOTE/${it.branch}")?.objectId
            when {
                localBranch == null -> LocalDivergence(0, 0)
                remoteBranch == null -> {
                    val defaultIds = DEFAULT_BRANCHES.mapNotNull(it::findRef).map { it.objectId }
                    it.revWalk().use {
                        defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                        it.markStart(it.parseCommit(localBranch))
                        val ahead = it.count().takeIf { it > 0 } ?: -1
                        LocalDivergence(ahead, 0)
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
                    LocalDivergence(
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
    fun divergenceDefault(repository: LocalRepository): Int {
        return repository.open("divergence default") {
            val head = it.resolve(it.branch)
            val defaultIds = DEFAULT_BRANCHES.mapNotNull(it::findRef).map { it.objectId }
            it.revWalk().use {
                defaultIds.map(it::parseCommit).forEach(it::markUninteresting)
                it.markStart(it.parseCommit(head))
                it.count()
            }
        }
    }

    /**
     * Creates a source code difference using `git diff` depending on the [file]'s status.
     */
    fun diff(repository: LocalRepository, file: LocalFile, lines: Int): String {
        return if (file.status == LocalFile.Status.CONFLICT && file.cached) ""
        else repository.openGit("diff cached=${file.cached} $file") {
            val diffCommand = it.diff()
                    .setCached(file.cached)
                    .setContextLines(lines)
                    .setPathFilter(file.toPathFilter())
            if (file.status == LocalFile.Status.CONFLICT) {
                val head = it.repository.resolve("$HEAD^{tree}")
                diffCommand.setOldTree(it.repository.newObjectReader().use { CanonicalTreeParser(null, it, head) })
                diffCommand.setNewTree(FileTreeIterator(it.repository))
            }
            ByteArrayOutputStream().use {
                diffCommand.setOutputStream(it).call()
                it.toString("UTF-8")
            }
        }
    }

    /**
     * - git diff --unified=<[lines]> <[commit]> <parent> <[file]>
     */
    fun diff(repository: LocalRepository, file: LocalFile, commit: LocalCommit, lines: Int): String {
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
            it.reset().setMode(ResetCommand.ResetType.HARD).setRef("$REMOTE/${it.repository.branch}").call()
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
     * - git rebase <[branch]>
     */
    fun rebase(repository: LocalRepository, branch: String) {
        repository.openGit("rebase $branch") { it.rebase().setUpstream(branch).call() }
    }

    /**
     * - git rebase --continue
     */
    fun rebaseContinue(repository: LocalRepository) {
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
    fun rebaseAbort(repository: LocalRepository) {
        repository.openGit("rebase abort") { it.rebase().setOperation(RebaseCommand.Operation.ABORT).call() }
    }

    /**
     * - git rebase --interactive <[baseId]>
     */
    fun rebaseSquash(repository: LocalRepository, baseId: String, message: String) {
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

    fun rebaseState(repository: LocalRepository): LocalRebase {
        val state = repository.open { it.repositoryState }
        if (state.isRebasing) {
            return when (state) {
                RepositoryState.REBASING, RepositoryState.REBASING_REBASING -> rebaseStateApply(repository)
                RepositoryState.REBASING_MERGE, RepositoryState.REBASING_INTERACTIVE -> rebaseStateMerge(repository)
                else -> throw IllegalStateException("Unexpected state $state")
            }
        } else {
            return LocalRebase(0, 0)
        }
    }

    private fun rebaseStateApply(repository: LocalRepository): LocalRebase {
        val rebasePath = "${repository.path}/.git/rebase-apply".asPath()
        val next = rebasePath.resolve("next").readFirst().toInt()
        val last = rebasePath.resolve("last").readFirst().toInt()
        return LocalRebase(next, last)
    }

    private fun rebaseStateMerge(repository: LocalRepository): LocalRebase {
        val rebasePath = "${repository.path}/.git/rebase-merge".asPath()
        val done = rebasePath.resolve("done").readLines().countMeaningful()
        val todo = rebasePath.resolve("git-rebase-todo").readLines().countMeaningful()
        return LocalRebase(done, done + todo)
    }

    private fun List<String>.countMeaningful() = filterNot { it.isBlank() || it.startsWith("#") }.size

    /**
     * - git checkout [branch]
     */
    fun checkout(repository: LocalRepository, branch: String) {
        if (branch == HEAD) return // cannot checkout HEAD directly
        repository.openGit("checkout $branch") { it.checkout().setName(branch).call() }
    }

    /**
     * - git checkout HEAD <[files]>
     */
    fun checkout(repository: LocalRepository, files: List<LocalFile>) {
        repository.openGit("checkout $files") {
            val checkoutCommand = it.checkout()
            files.forEach { checkoutCommand.addPath(it.path) }
            checkoutCommand.call()
        }
    }

    /**
     * - git checkout -b <local> --track <[remote]>
     */
    fun checkoutRemote(repository: LocalRepository, remote: String) {
        if (remote.substringAfter('/') == HEAD) return // cannot checkout HEAD directly
        repository.openGit("checkout $remote") {
            it.checkout()
                    .setCreateBranch(true)
                    .setName(remote.substringAfter('/'))
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
        return repository.openGit("stash list") { it.stashList().call().map { LocalStashEntry(it.name, it.fullMessage) } }
    }

    /**
     * - git stash list
     */
    fun stashListSize(repository: LocalRepository): Int {
        return repository.openGit("stash list size") { it.stashList().call().size }
    }

    /**
     * - git fetch
     */
    fun fetch(repository: LocalRepository) {
        cache -= repository
        repository.openGit("fetch") { it.fetch(repository) }
    }

    /**
     * - git fetch --prune
     * - git gc --aggressive
     */
    fun fetchGc(repository: LocalRepository) {
        cache -= repository
        repository.openGit("fetch prune gc") {
            it.fetch().applyAuth(repository).setRemoveDeletedRefs(true).call()
            // TODO: clarify when to do
            try {
                it.gc().setAggressive(true).call()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        cache += repository
    }

    private fun JGit.fetch(repository: LocalRepository) {
        if (!isUpdated(repository)) {
            fetch().applyAuth(repository).call()
            cache += repository
        }
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

    private fun <C : GitCommand<T>, T> TransportCommand<C, T>.applyAuth(repository: LocalRepository): C {
        val credentials = GitCredentials(repository.ssh, repository.username, repository.password)
        return if (credentials.isSSH) setTransportConfigCallback(credentials.sshTransport)
        else setCredentialsProvider(credentials.userCredentials)
    }

    private inline fun <T> LocalRepository.open(description: String = "", block: (Repository) -> T): T {
        Git.proxyHost.set(proxyHost)
        Git.proxyPort.set(proxyPort)
        val key = RepositoryCache.FileKey.lenient(File(path), FS.DETECTED)
        return stopTime(shortPath, description) {
            RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build().let(block)
        }
    }

    private inline fun <T> LocalRepository.openGit(description: String = "", block: (JGit) -> T) = open(description) { JGit(it).let(block) }

    private fun Repository.revWalk() = RevWalk(this)

    // TODO: test performance if objectreader is created here instead
    private fun Repository.treesOf(commitId: AnyObjectId): Pair<AbstractTreeIterator, AbstractTreeIterator> {
        return revWalk().use {
            val commit = it.parseCommit(commitId).takeIf { it.parentCount < 2 }
            val parent = commit?.parents?.firstOrNull()?.let(it::parseCommit)
            it.iteratorOf(commit) to it.iteratorOf(parent)
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
            FollowFilter.create(path, config.get(DiffConfig.KEY))
        } else {
            PathFilter.create(path)
        }
    }

}
