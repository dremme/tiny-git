package hamburg.remme.tinygit

import javafx.application.Application
import javafx.application.Platform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("Testing application")
internal class TinyGitApplicationIntegrationTest {

    @Test
    @DisplayName("Smoke test")
    fun smokeTest() {
        // Given
        val appClass = TinyGitApplication::class.java

        // Then; comes before 'When' because launch is blocking
        Executors.newSingleThreadScheduledExecutor().schedule({ Platform.exit() }, 5, TimeUnit.SECONDS)

        // When
        Application.launch(appClass)
    }

}
