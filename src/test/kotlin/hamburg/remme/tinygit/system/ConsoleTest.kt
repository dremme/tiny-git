package hamburg.remme.tinygit.system

import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.system.git.GIT
import hamburg.remme.tinygit.system.git.LOG
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTimeout
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import java.time.Duration.ofMillis

@DisplayName("Testing native console")
internal class ConsoleTest {

    @DisplayName("Testing execute")
    @CsvSource(".,build.gradle", "./image,image1.png")
    @ParameterizedTest fun testExecute(path: String, file: String) {
        // Given
        val args = listOf("ls")
        val collector = ConsoleCollector()
        val dir = File(path)

        // When
        Console.execute(dir, args, collector::collect)

        // Then
        assertThat(collector.lines).contains(file)
    }

    @DisplayName("Testing execute performance")
    @Test fun testPerformance() {
        // Given
        val args = listOf(GIT, LOG, "-100")

        // Then
        assertTimeout(ofMillis(100)) {
            // When
            Console.execute(CURRENT_DIR, args).readLines()
        }
    }

}
