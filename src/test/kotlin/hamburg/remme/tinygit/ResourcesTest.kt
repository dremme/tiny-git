package hamburg.remme.tinygit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing resource utils")
internal class ResourcesTest {

    @DisplayName("Testing resource as URL")
    @Test fun testURL() {
        // Given
        val url = "/application.yml"

        // When
        val resource = url.toURL()

        // Then
        assertThat(resource.file).contains(url)
    }

    @DisplayName("Testing resource as stream")
    @Test fun testStream() {
        // Given
        val url = "/application.yml"

        // When
        val resource = url.openStream().bufferedReader().useLines { it.joinToString("\n") }

        // Then
        assertThat(resource).contains("icon.png")
    }

}
