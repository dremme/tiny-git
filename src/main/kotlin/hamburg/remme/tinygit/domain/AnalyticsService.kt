package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.system.git.CommitProperty
import org.springframework.stereotype.Service
import java.io.File

/**
 * A service responsible for analytics.
 */
@Service class AnalyticsService(private val service: RepositoryService) {

    /**
     * Groups the given commit property by number of occurrence.
     * @param gitDir   a local Git repository.
     * @param property the property to group.
     * @return key-value-pairs of the property value and number of occurrences.
     */
    fun group(gitDir: File, property: CommitProperty): Map<Any, Int> {
        return service.list(gitDir).mapNotNull { it[property] }.groupingBy { it }.eachCount()
    }

    /**
     * Lists all unique occurrences of a commit property.
     * @param gitDir   a local Git repository.
     * @param property the property to list.
     * @return list of unique values.
     */
    fun listUnique(gitDir: File, property: CommitProperty): List<Any> {
        return service.list(gitDir).mapNotNull { it[property] }.distinct()
    }

}
