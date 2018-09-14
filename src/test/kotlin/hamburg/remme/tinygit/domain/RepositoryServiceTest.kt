package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.event.RepositoryUpdateRequestedEvent
import hamburg.remme.tinygit.event.RepositoryUpdatedEvent
import hamburg.remme.tinygit.spyOn
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.system.git.GitLog
import hamburg.remme.tinygit.system.git.GitRemote
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

@DisplayName("Testing repository service")
@ExtendWith(MockitoExtension::class)
internal class RepositoryServiceTest {

    @Mock lateinit var gitLog: GitLog
    @Mock lateinit var gitRemote: GitRemote
    @Mock lateinit var publisher: ApplicationEventPublisher
    @Captor lateinit var eventCaptor: ArgumentCaptor<RepositoryUpdatedEvent>
    private lateinit var service: RepositoryService

    private val result = listOf(Commit("12345678"))

    @BeforeEach fun setup() {
        whenever(gitLog.query(CURRENT_DIR)).thenReturn(result)
        service = spyOn(RepositoryService(gitLog, gitRemote, publisher))
    }

    @DisplayName("Testing list")
    @Test fun testList() {
        // When
        val list = service.list(CURRENT_DIR)

        // Then
        verify(gitLog).query(CURRENT_DIR)
        assertThat(list).isEqualTo(result)
    }

    @DisplayName("Testing update")
    @Test fun testUpdate() {
        // When
        service.update(CURRENT_DIR)

        // Then
        verify(gitRemote).pull(CURRENT_DIR)
        verify(gitLog).invalidateCache()
    }

    @DisplayName("Testing handle update request")
    @Test fun testHandleRequestUpdate() {
        // Given
        val event = RepositoryUpdateRequestedEvent(CURRENT_DIR)

        // When
        service.handleRequestUpdate(event)

        // Then
        verify(service).update(CURRENT_DIR)
        verify(publisher).publishEvent(eventCaptor.capture())
        assertThat(eventCaptor.value.directory).isEqualTo(CURRENT_DIR)
    }

}
