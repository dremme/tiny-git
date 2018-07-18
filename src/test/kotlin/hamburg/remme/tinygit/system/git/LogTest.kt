package hamburg.remme.tinygit.system.git

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing Git log")
internal class LogTest {

    @Test
    @DisplayName("Testing Git log simple query")
    fun testQuery() {
        // When
        val result = Log.query()

        // Then
        assertThat(result.commits.size).isGreaterThan(1)
        assertThat(result.commits[0]).matches("[a-f0-9]{40}")
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
