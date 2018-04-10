package hamburg.remme.tinygit.domain

import java.util.concurrent.locks.ReentrantLock

class LogGraph {

    private val lock = ReentrantLock()
    private val backingList = mutableListOf<LogBranch>()
    private lateinit var commits: List<Commit>

    fun recreate(commits: List<Commit>) = synchronized(lock) {
        backingList.clear()
        this.commits = commits
        this.commits.forEachIndexed { i, it -> createFlow(it, i) }
    }

    fun getTag(commit: Commit) = getTag(commit.id)

    fun getTag(commit: CommitIsh) = getTag(commit.id)

    private fun getTag(id: String) = synchronized(lock) {
        backingList.find { it.any { it == id } }?.tag ?: -1
    }

    fun getHighestTag() = synchronized(lock) {
        backingList.map { it.tag }.max() ?: -1
    }

    private fun contains(commit: Commit) = backingList.any { it.contains(commit.id) }

    private fun contains(commit: CommitIsh) = backingList.any { it.contains(commit.id) }

    private fun createFlow(commit: Commit, commitIndex: Int) {
        if (!contains(commit)) {
            val tags = backingList.filter { it.start <= commitIndex && it.end >= commitIndex }.map { it.tag }
            val max = tags.max() ?: 0
            val tag = (0..max).firstOrNull { !tags.contains(it) } ?: max+1

            val branch = LogBranch(tag, commitIndex)
            backingList += branch

            commit.more(branch)
        }
    }

    private fun Commit.more(branch: LogBranch) {
        branch += id
        val parent = parents.firstOrNull()
        if (parent == null) branch.finish(this)
        else if (!contains(parent)) parent.peel()?.more(branch) ?: branch.indeterminate(parent)
        else branch.finish(parent)
    }

    private fun CommitIsh.peel() = commits.firstOrNull { it.id == id }

    private inner class LogBranch(val tag: Int, val start: Int) : ArrayList<String>() {

        var end = 9999 // arbitrary end; TODO: should prob be higher

        fun finish(commit: Commit) {
            end = commits.indexOf(commit)
        }

        fun finish(commit: CommitIsh) {
            commit.peel()?.let { end = commits.indexOf(it) }
        }

        fun indeterminate(commit: CommitIsh) {
            add(commit.id)
        }

    }

}
