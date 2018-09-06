package hamburg.remme.tinygit.system.git

import hamburg.remme.tinygit.CURRENT_DIR
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Testing Git remote actions")
internal class GitRemoteTest {

    private lateinit var gitRemote: GitRemote

    @BeforeEach fun setup() {
        gitRemote = GitRemote()
    }

    @DisplayName("Testing Git pull")
    @Test fun testPull() {
        // When
        val message = gitRemote.pull(CURRENT_DIR)

        // Then
        assertThat(message).containsIgnoringCase("already up to date")
    }

}
