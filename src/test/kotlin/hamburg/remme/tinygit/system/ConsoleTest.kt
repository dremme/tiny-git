package hamburg.remme.tinygit.system

import hamburg.remme.tinygit.system.git.LOG
import hamburg.remme.tinygit.system.git.VERSION
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration.ofMillis

@DisplayName("Testing native console")
internal class ConsoleTest {

    @ParameterizedTest
    @CsvSource(",build.gradle", "./image,image1.png")
    @DisplayName("Testing execute")
    fun testExecute(workingDir: String?, file: String) {
        // Given
        val args = arrayOf("ls")
        val collector = ConsoleCollector()

        // When
        Console.execute(workingDir, args, collector::collect)

        // Then
        assertThat(collector.lines).contains(file)
    }

    @Test
    @DisplayName("Testing git returned all as one string")
    fun testGit() {
        // Given
        val args = arrayOf(VERSION)

        // When
        val result = Console.git(*args)

        // Then
        assertThat(result).matches("git version \\d+\\.\\d+\\.\\d+")
    }

    @Test
    @DisplayName("Testing git with block")
    fun testGitBlock() {
        // Given
        val args = arrayOf(LOG, "--oneline", "-10")
        val collector = ConsoleCollector()

        // When
        Console.git(*args, block = collector::collect)

        // Then
        assertThat(collector.lines)
                .hasSize(10)
                .allMatch { it.matches("[a-f0-9]+ .*".toRegex()) }
    }

    @Test
    @DisplayName("Testing execute performance")
    fun testPerformance() {
        // Given
        val args = arrayOf(LOG, "-100")

        // Then
        assertTimeout(ofMillis(100)) {
            // When
            Console.git(*args)
        }
    }

}
