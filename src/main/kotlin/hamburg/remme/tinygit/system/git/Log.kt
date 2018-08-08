package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.safeSplit
import hamburg.remme.tinygit.system.Console
import hamburg.remme.tinygit.toInstant
import org.springframework.stereotype.Component
import java.util.BitSet

/**
 * Reads Git logs and returns commits and general log info.
 */
@Component class Log {

    /**
     * Returns all commit IDs in the Git repository in order of commit creation.
     */
    fun query(): Result {
        val parser = LogParser()
        Console.git(LOG, "--all", "--pretty=format:$LOG_PATTERN", block = parser::append)
        return parser.commits
    }

    /**
     * Private parser to parse git log output.
     */
    private class LogParser {

        val commits = arrayListOf<Map<CommitProperty, Any>>()
        private val bits = BitSet(LOG_PATTERN_LINES)
        private val properties = mutableMapOf<CommitProperty, Any>()

        /**
         * Parses a line depending on which bit index the line is at.
         */
        fun append(line: String) {
            when (bits.cardinality()) {
                0 -> properties[CommitProperty.H] = line
                1 -> properties[CommitProperty.h] = line
                2 -> properties[CommitProperty.P] = line.safeSplit()
                3 -> properties[CommitProperty.p] = line.safeSplit()
                4 -> properties[CommitProperty.ae] = line
                5 -> properties[CommitProperty.an] = line
                6 -> properties[CommitProperty.at] = line.toInstant()
                7 -> properties[CommitProperty.ce] = line
                8 -> properties[CommitProperty.cn] = line
                9 -> properties[CommitProperty.ct] = line.toInstant()
            }
            increment()

            if (bits.cardinality() == LOG_PATTERN_LINES) {
                commits += properties
                clear()
            }
        }

        private fun clear() = bits.clear()

        private fun increment() = bits.set(bits.previousSetBit(LOG_PATTERN_LINES) + 1)

    }

}
