package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock


@DisplayName("Testing analytics service")
@ExtendWith(MockitoExtension::class)
internal class AnalyticsServiceTest {

    @Mock lateinit var repositoryService: RepositoryService
    private lateinit var service: AnalyticsService

    private val frodoEmail = "frodo.baggins@shire.me"
    private val samEmail = "samwise.gamgee@shire.me"
    private val samName = "Samwise Gamgee"
    private val result = listOf(
      Commit(authorEmail = frodoEmail),
      Commit(authorEmail = frodoEmail),
      Commit(authorEmail = samEmail, authorName = samName)
    )

    @BeforeEach fun setup() {
        whenever(repositoryService.list(CURRENT_DIR)).thenReturn(result)
        service = AnalyticsService(repositoryService)
    }

    @DisplayName("Testing property grouping")
    @Test fun testGroup() {
        // Given
        val property = Commit::authorEmail

        // When
        val grouping = service.group(CURRENT_DIR, property)

        // Then
        assertThat(grouping)
          .hasSize(2)
          .containsEntry(frodoEmail, 2)
          .containsEntry(samEmail, 1)
          .doesNotContainKey(samName)
    }

    @DisplayName("Testing unique counting")
    @Test fun testCountUnique() {
        // Given
        val property = Commit::authorEmail

        // When
        val list = service.listUnique(CURRENT_DIR, property)

        // Then
        assertThat(list)
          .hasSize(2)
          .contains(frodoEmail)
          .contains(samEmail)
    }

}
