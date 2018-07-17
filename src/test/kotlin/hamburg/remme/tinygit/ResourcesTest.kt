package hamburg.remme.tinygit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing resource utils")
internal class ResourcesTest {

    @Test
    @DisplayName("Testing resource as URL")
    fun testResource() {
        // Given
        val url = "/application.yml"

        // When
        val resource = resource(url)

        // Then
        assertThat(resource.file).contains(url)
    }

    @Test
    @DisplayName("Testing resource as stream")
    fun testResourceStream() {
        // Given
        val url = "/application.yml"

        // When
        val resource = resourceStream(url).bufferedReader().useLines { it.joinToString("\n") }

        // Then
        assertThat(resource).contains("icon.png")
    }

    @Test
    @DisplayName("Testing resource as string")
    fun testResourceString() {
        // Given
        val url = "/application.yml"

        // When
        val resource = resourceString(url)

        // Then
        assertThat(resource).contains(url)
    }

}
