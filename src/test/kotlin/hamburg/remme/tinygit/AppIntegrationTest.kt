package hamburg.remme.tinygit

import javafx.application.Application
import javafx.application.Platform
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("Testing application")
@Disabled("Disabled because there is no integration pipeline yet")
internal class AppIntegrationTest {

    @DisplayName("Smoke test")
    @Test fun smokeTest() {
        // Given
        val appClass = GitAnalytics::class.java

        // Then; comes before 'When' because launch is blocking
        Executors.newSingleThreadScheduledExecutor().schedule({ Platform.exit() }, 5, TimeUnit.SECONDS)

        // When
        Application.launch(appClass)
    }

}
