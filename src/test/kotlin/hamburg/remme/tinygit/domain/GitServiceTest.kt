package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.system.git.CommitProperty
import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@ExtendWith(MockitoExtension::class)
internal class GitServiceTest {

    @Mock lateinit var log: Log
    lateinit var service: GitService
    private val result = listOf(mapOf(CommitProperty.H to "12345678"))

    @BeforeEach
    fun setup() {
        whenever(log.query()).thenReturn(result)
        service = GitService(log)
    }

    @Test
    @DisplayName("Testing list")
    fun testList() {
        // When
        val list = service.list()

        // Then
        verify(log).query()
        assertThat(list).isEqualTo(result)
    }

    @Test
    @DisplayName("Testing count")
    fun testCount() {
        // When
        val count = service.count()

        // Then
        verify(log).query()
        assertThat(count).isEqualTo(result.size)
    }

    @Test
    @DisplayName("Testing log cache")
    fun testCache() {
        // Given
        service.list()

        // When
        service.list()

        // Then
        verify(log).query()
    }

    @Test
    @DisplayName("Testing cache invalidation")
    fun testInvalidateCache() {
        // Given
        service.list()

        // When
        service.invalidateCache()
        service.list()

        // Then
        verify(log, times(2)).query()
    }

}
