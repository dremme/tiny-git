package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.Commit
import org.springframework.stereotype.Service
import java.io.File

/**
 * A service responsible for analytics.
 */
@Service class AnalyticsService(private val service: RepositoryService) {

    /**
     * Groups the given commit property by number of occurrence.
     * @param gitDir a local Git repository.
     * @param block  block to extract the property.
     * @return key-value-pairs of the property value and number of occurrences.
     */
    fun <T> group(gitDir: File, block: (Commit) -> T): Map<T, Int> {
        return service.list(gitDir).map(block).groupingBy { it }.eachCount()
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

}
