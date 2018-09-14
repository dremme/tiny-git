package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.untilNow
import org.springframework.stereotype.Service
import java.io.File
import java.time.temporal.ChronoUnit

/**
 * A service responsible for analytics.
 */
@Service class AnalyticsService(private val service: RepositoryService, private val settings: Settings) {

    /**
     * @param gitDir a local Git repository.
     * @return the count of all commits in the current repository.
     */
    fun count(gitDir: File): Int {
        return service.list(gitDir).size
    }

    /**
     * Groups the given commit property by number of occurrence.
     * @param gitDir a local Git repository.
     * @param block  block to extract the property.
     * @return key-value-pairs of the property value and number of occurrences.
     */
    fun <T> group(gitDir: File, block: (Commit) -> T): Map<T, Int> {
        return service.list(gitDir).groupingBy(block).eachCount()
    }

    /**
     * Lists all unique occurrences of a commit property.
     * @param gitDir a local Git repository.
     * @param block  block to extract the property.
     * @return list of unique values.
     */
    fun <T> listUnique(gitDir: File, block: (Commit) -> T): List<T> {
        return service.list(gitDir).map(block).distinct()
    }

    /**
     * Counts all unique occurrences of a commit property.
     * @param gitDir a local Git repository.
     * @param block  block to extract the property.
     * @return number of unique values.
     */
    fun <T> countUnique(gitDir: File, block: (Commit) -> T): Int {
        return listUnique(gitDir, block).size
    }

    /**
     * Calculates the age of a Git repository by taking the date of the first commit.
     * @param gitDir a local Git repository.
     * @param unit   the unit to measure the age in.
     * @return the age of the repository in the given unit.
     */
    fun age(gitDir: File, unit: ChronoUnit): Long {
        return service.list(gitDir).last().committerTime.untilNow(unit, settings.timeZone)
    }

}
