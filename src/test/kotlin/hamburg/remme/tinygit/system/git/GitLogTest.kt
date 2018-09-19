package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.COMMIT_ID_PATTERN
import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.MAIL_PATTERN
import hamburg.remme.tinygit.SHORT_COMMIT_ID_PATTERN
import hamburg.remme.tinygit.spyOn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Testing Git log")
internal class GitLogTest {

    private lateinit var gitLog: GitLog

    @BeforeEach fun setup() {
        gitLog = spyOn(GitLog())
    }

    @DisplayName("Testing Git log")
    @Test fun testLog() {
        // Given
        val now = Instant.now().minus(1, ChronoUnit.MINUTES)
        val then = now.minus(3650, ChronoUnit.DAYS)

        // When
        val result = gitLog.log(CURRENT_DIR)

        // Then
        assertThat(result)
          .isNotEmpty
          .allSatisfy {
              assertThat(it.id).matches(COMMIT_ID_PATTERN)
              assertThat(it.shortId).matches(SHORT_COMMIT_ID_PATTERN)
              assertThat(it.parents).allMatch { it.matches(COMMIT_ID_PATTERN.toRegex()) }
              assertThat(it.parents).hasSameSizeAs(it.shortParents)
              assertThat(it.shortParents).allMatch { it.matches(SHORT_COMMIT_ID_PATTERN.toRegex()) }
              assertThat(it.authorEmail).matches(MAIL_PATTERN)
              assertThat(it.authorName).isNotBlank()
              assertThat(it.authorTime).isBetween(then, now)
              assertThat(it.committerEmail).matches(MAIL_PATTERN)
              assertThat(it.committerName).isNotBlank()
              assertThat(it.committerTime).isBetween(then, now)
              assertThat(it.message).isNotBlank()
          }
    }

    @DisplayName("Testing Git log caching")
    @Test fun testCaching() {
        // When
        gitLog.query(CURRENT_DIR)
        gitLog.query(CURRENT_DIR)

        // Then
        verify(gitLog).log(CURRENT_DIR)
    }

    @DisplayName("Testing log cache invalidation")
    @Test fun testInvalidateCache() {
        // Given
        gitLog.query(CURRENT_DIR)

        // When
        gitLog.invalidateCache()

        // Then
        gitLog.query(CURRENT_DIR)
        verify(gitLog, times(2)).log(CURRENT_DIR)
    }

}
