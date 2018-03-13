package hamburg.remme.tinygit.domain

import java.util.concurrent.locks.ReentrantLock

class LogGraph {

    private val lock = ReentrantLock()
    private val backingList = mutableListOf<Branch>()
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

            val branch = Branch(tag, commitIndex)
            backingList.add(branch)

            commit.more(branch)
        }
    }

    private fun Commit.more(branch: Branch) {
        branch.add(id)
        parents.firstOrNull()
                ?.let {
                    if (!contains(it)) it.peel()?.more(branch) ?: branch.indeterminate(it)
                    else branch.finish(it)
                }
                ?: branch.finish(this)
    }

    private fun CommitIsh.peel() = commits.firstOrNull { it.id == id }

    private inner class Branch(val tag: Int, val start: Int, var end: Int = 9999) : ArrayList<String>() {

        fun finish(commit: Commit) {
            end = commits.indexOf(commit)
        }

        fun finish(commit: CommitIsh) {
            end = commit.peel()?.let { commits.indexOf(it) } ?: 9999
        }

        fun indeterminate(commit: CommitIsh) {
            add(commit.id)
        }

    }

}
