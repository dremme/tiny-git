package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.COMMIT_ID_PATTERN
import hamburg.remme.tinygit.COMMIT_SHORT_ID_PATTERN
import hamburg.remme.tinygit.MAIL_PATTERN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Testing Git log")
internal class LogTest {

    @Test
    @DisplayName("Testing Git log simple query")
    fun testQuery() {
        // Given
        val now = Instant.now().minus(1, ChronoUnit.MINUTES)
        val then = now.minus(3650, ChronoUnit.DAYS)

        // When
        val result = Log.query()

        // Then
        assertThat(result.commits)
                .isNotEmpty
                .allSatisfy {
                    assertThat(it.id).matches(COMMIT_ID_PATTERN)
                    assertThat(it.shortId).matches(COMMIT_SHORT_ID_PATTERN)
                    assertThat(it.parentIds).allMatch { it.matches(COMMIT_ID_PATTERN.toRegex()) }
                    assertThat(it.parentIds).hasSameSizeAs(it.parentShortIds)
                    assertThat(it.parentShortIds).allMatch { it.matches(COMMIT_SHORT_ID_PATTERN.toRegex()) }
                    assertThat(it.authorMail).matches(MAIL_PATTERN)
                    assertThat(it.authorName).isNotBlank()
                    assertThat(it.authorDate).isBetween(then, now)
                    assertThat(it.committerMail).matches(MAIL_PATTERN)
                    assertThat(it.committerName).isNotBlank()
                    assertThat(it.committerDate).isBetween(then, now)
                }
    }

    @Test
    @DisplayName("Testing Git log commit count")
    fun testCount() {
        // When
        val result = Log.count()

        // Then
        assertThat(result).isGreaterThan(1)
    }

}
