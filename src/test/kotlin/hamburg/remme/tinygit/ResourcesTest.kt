package hamburg.remme.tinygit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing resource utils")
internal class ResourcesTest {

    @Test
    @DisplayName("Testing resource as URL")
    fun testURL() {
        // Given
        val url = "/application.yml"

        // When
        val resource = url.toURL()

        // Then
        assertThat(resource.file).contains(url)
    }

    @Test
    @DisplayName("Testing resource as stream")
    fun testStream() {
        // Given
        val url = "/application.yml"

        // When
        val resource = url.openStream().bufferedReader().useLines { it.joinToString("\n") }

        // Then
        assertThat(resource).contains("icon.png")
    }

}
