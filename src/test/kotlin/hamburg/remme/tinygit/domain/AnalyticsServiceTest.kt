package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.Settings
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.toDateTime
import hamburg.remme.tinygit.toInstant
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@DisplayName("Testing analytics service")
@ExtendWith(MockitoExtension::class)
internal class AnalyticsServiceTest {

    @Mock lateinit var repositoryService: RepositoryService
    private val settings = Settings()
    private lateinit var service: AnalyticsService

    private val frodoEmail = "frodo.baggins@shire.me"
    private val samEmail = "samwise.gamgee@shire.me"
    private val samName = "Samwise Gamgee"
    private val result = sequenceOf(
      Commit(authorEmail = frodoEmail, committerTime = "1514980800".toInstant()),
      Commit(authorEmail = frodoEmail, committerTime = "1514894400".toInstant()),
      Commit(authorEmail = samEmail, authorName = samName, committerTime = "1514808000".toInstant())
    )

    @BeforeEach fun setup() {
        whenever(repositoryService.list(CURRENT_DIR)).thenReturn(result)
        service = AnalyticsService(repositoryService, settings)
    }

    @DisplayName("Testing count")
    @Test fun testCount() {
        // When
        val count = service.count(CURRENT_DIR)

        // Then
        assertThat(count).isEqualTo(result.count())
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

    @DisplayName("Testing unique list")
    @Test fun testListUnique() {
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

    @DisplayName("Testing unique count")
    @Test fun testCountUnique() {
        // Given
        val property = Commit::authorEmail

        // When
        val count = service.countUnique(CURRENT_DIR, property)

        // Then
        assertThat(count).isEqualTo(2)
    }

    @DisplayName("Testing age")
    @Test fun testAge() {
        // Given
        val unit = ChronoUnit.DAYS
        val daysBetween = unit.between(result.last().committerTime.toDateTime(ZoneId.systemDefault()), LocalDateTime.now())

        // When
        val count = service.age(CURRENT_DIR, unit)

        // Then
        assertThat(count).isEqualTo(daysBetween)
    }

}
