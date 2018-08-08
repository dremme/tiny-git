package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.system.git.CommitProperty
import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Remote
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@DisplayName("Testing Git service")
@ExtendWith(MockitoExtension::class)
internal class GitServiceTest {

    @Mock lateinit var log: Log
    @Mock lateinit var remote: Remote
    private lateinit var service: GitService
    private val result = listOf(mapOf(CommitProperty.H to "12345678"))

    @BeforeEach fun setup() {
        whenever(log.query()).thenReturn(result)
        service = GitService(log, remote)
    }

    @DisplayName("Testing list")
    @Test fun testList() {
        // When
        val list = service.list()

        // Then
        verify(log).query()
        assertThat(list).isEqualTo(result)
    }

    @DisplayName("Testing count")
    @Test fun testCount() {
        // When
        val count = service.count()

        // Then
        verify(log).query()
        assertThat(count).isEqualTo(result.size)
    }

    @DisplayName("Testing log cache")
    @Test fun testCache() {
        // Given
        service.list()

        // When
        service.list()

        // Then
        verify(log).query()
    }

    @DisplayName("Testing cache invalidation")
    @Test fun testInvalidateCache() {
        // Given
        service.list()

        // When
        service.invalidateCache()
        service.list()

        // Then
        verify(log, times(2)).query()
    }

    @DisplayName("Testing update")
    @Test fun testUpdate() {
        // When
        service.update()

        // Then
        verify(remote).pull()
    }

}
