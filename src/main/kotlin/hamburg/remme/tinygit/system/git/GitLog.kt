package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.safeSplit
import hamburg.remme.tinygit.system.cmd
import hamburg.remme.tinygit.toInstant
import org.springframework.stereotype.Component
import java.io.File
import java.time.Instant
import java.util.BitSet

/**
 * Reads Git logs and returns commits and general log info.
 */
@Component class GitLog {

    /**
     * Invalidates the log cache.
     */
    fun invalidateCache() {
        // TODO
    }

    /**
     * @param gitDir a local Git repository.
     * @return all commit IDs in the Git repository in order of commit creation.
     */
    fun query(gitDir: File): Sequence<Commit> {
        val parser = LogParser()
        cmd(gitDir, listOf(GIT, LOG, ALL, "--pretty=format:$LOG_PATTERN")).forEachLine(parser::append)
        return parser.commits.asSequence() // FIXME: pretty dirty for now
    }

    /**
     * Private parser to parse `git log` output.
     */
    private class LogParser {

        val commits = mutableListOf<Commit>()
        private val bits = BitSet(LOG_PATTERN_LINES)
        private val builder = CommitBuilder()

        /**
         * Parses a line depending on which bit index the line is at.
         */
        fun append(line: String) {
            when (bits.cardinality()) {
                0 -> builder.id = line
                1 -> builder.shortId = line
                2 -> builder.parents = line.safeSplit()
                3 -> builder.shortParents = line.safeSplit()
                4 -> builder.authorEmail = line
                5 -> builder.authorName = line
                6 -> builder.authorTime = line.toInstant()
                7 -> builder.committerEmail = line
                8 -> builder.committerName = line
                9 -> builder.committerTime = line.toInstant()
                10 -> builder.message = line
            }
            increment()

            if (bits.cardinality() == LOG_PATTERN_LINES) {
                commits += builder.build()
                clear()
            }
        }

        private fun clear() = bits.clear()

        private fun increment() = bits.set(bits.previousSetBit(LOG_PATTERN_LINES) + 1)

    }

    /**
     * Builder for convenience.
     */
    private class CommitBuilder(var id: String = "",
                                var shortId: String = "",
                                var parents: List<String> = emptyList(),
                                var shortParents: List<String> = emptyList(),
                                var authorEmail: String = "",
                                var authorName: String = "",
                                var authorTime: Instant = Instant.now(),
                                var committerEmail: String = "",
                                var committerName: String = "",
                                var committerTime: Instant = Instant.now(),
                                var message: String = "") {

        /**
         * Creates a new commit from the builder's properties.
         */
        fun build() = Commit(id, shortId, parents, shortParents, authorEmail, authorName, authorTime,
          committerEmail, committerName, committerTime, message)

    }

}
