package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.COMMIT_ID_PATTERN
import hamburg.remme.tinygit.MAIL_PATTERN
import hamburg.remme.tinygit.SHORT_COMMIT_ID_PATTERN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

@DisplayName("Testing Git log")
internal class LogTest {

    lateinit var log: Log

    @BeforeEach
    fun setup() {
        log = Log()
    }

    @Test
    @DisplayName("Testing Git log simple query")
    @Suppress("UNCHECKED_CAST", "NestedLambdaShadowedImplicitParameter")
    fun testQuery() {
        // Given
        val now = Instant.now().minus(1, ChronoUnit.MINUTES)
        val then = now.minus(3650, ChronoUnit.DAYS)

        // When
        val result = log.query()

        // Then
        assertThat(result)
                .isNotEmpty
                .allSatisfy {
                    assertThat(it[CommitProperty.H] as String).matches(COMMIT_ID_PATTERN)
                    assertThat(it[CommitProperty.h] as String).matches(SHORT_COMMIT_ID_PATTERN)
                    assertThat(it[CommitProperty.P] as List<String>).allMatch { it.matches(COMMIT_ID_PATTERN.toRegex()) }
                    assertThat(it[CommitProperty.P] as List<String>).hasSameSizeAs(it[CommitProperty.p] as List<String>)
                    assertThat(it[CommitProperty.p] as List<String>).allMatch { it.matches(SHORT_COMMIT_ID_PATTERN.toRegex()) }
                    assertThat(it[CommitProperty.ae] as String).matches(MAIL_PATTERN)
                    assertThat(it[CommitProperty.an] as String).isNotBlank()
                    assertThat(it[CommitProperty.at] as Instant).isBetween(then, now)
                    assertThat(it[CommitProperty.ce] as String).matches(MAIL_PATTERN)
                    assertThat(it[CommitProperty.cn] as String).isNotBlank()
                    assertThat(it[CommitProperty.ct] as Instant).isBetween(then, now)
                }
    }

}
