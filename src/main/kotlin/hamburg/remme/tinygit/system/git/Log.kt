package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.system.Console
import java.time.Instant
import java.util.BitSet

/**
 * Reads Git logs and returns commits and general log info.
 */
object Log {

    /**
     * Returns all commit IDs in the Git repository in order of commit creation.
     */
    fun query(): LogResult {
        val parser = LogParser()
        Console.git(LOG, "--all", "--pretty=format:$LOG_PATTERN", block = parser::append)
        return LogResult(parser.commits)
    }

    /**
     * Returns the count of commits in the Git repository.
     */
    fun count(): Int {
        return Console.git(REV_LIST, "--all", "--count").toInt()
    }

    /**
     * Private parser to parse git log output.
     */
    private class LogParser {

        val commits = arrayListOf<Commit>()
        private val bits = BitSet(LOG_PATTERN_LINES)
        // The fields are named after the placeholders
        @Suppress("PrivatePropertyName")
        private lateinit var H: String
        private lateinit var h: String
        @Suppress("PrivatePropertyName")
        private lateinit var P: List<String>
        private lateinit var p: List<String>
        private lateinit var ae: String
        private lateinit var an: String
        private lateinit var at: Instant
        private lateinit var ce: String
        private lateinit var cn: String
        private lateinit var ct: Instant

        /**
         * Parses a line depending on which bit index the line is at.
         */
        fun append(line: String) {
            when (bits.cardinality()) {
                0 -> H = line
                1 -> h = line
                2 -> P = line.takeIf(String::isNotBlank)?.split(" ") ?: emptyList()
                3 -> p = line.takeIf(String::isNotBlank)?.split(" ") ?: emptyList()
                4 -> ae = line
                5 -> an = line
                6 -> at = Instant.ofEpochSecond(line.toLong())
                7 -> ce = line
                8 -> cn = line
                9 -> ct = Instant.ofEpochSecond(line.toLong())
            }
            increment()

            if (bits.cardinality() == LOG_PATTERN_LINES) {
                commits += newCommit()
                clear()
            }
        }

        private fun clear() = bits.clear()

        private fun increment() = bits.set(bits.previousSetBit(LOG_PATTERN_LINES) + 1)

        private fun newCommit() = Commit(H, h, P, p, ae, an, at, ce, cn, ct)

    }

}
