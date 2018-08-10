package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.CURRENT_DIR
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing Git remote actions")
internal class RemoteTest {

    private lateinit var remote: Remote

    @BeforeEach fun setup() {
        remote = Remote()
    }

    @DisplayName("Testing Git pull")
    @Test fun testPull() {
        // When
        val message = remote.pull(CURRENT_DIR)

        // Then
        assertThat(message).containsIgnoringCase("already up to date")
    }

}
