package hamburg.remme.tinygit.git

import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk

fun RevWalk.commits(): Iterator<RevCommit> {
    return object : Iterator<RevCommit> {
        var commit: RevCommit? = this@commits.next()

        override fun hasNext() = commit != null

        override fun next(): RevCommit {
            if (commit == null) throw NoSuchElementException()
            val result = commit!!
            commit = this@commits.next()
            return result
        }
    }
}
