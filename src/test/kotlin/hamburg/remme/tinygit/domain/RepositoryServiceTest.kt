package hamburg.remme.tinygit.domain

import hamburg.remme.tinygit.CURRENT_DIR
import hamburg.remme.tinygit.MockitoExtension
import hamburg.remme.tinygit.system.git.Commit
import hamburg.remme.tinygit.system.git.Log
import hamburg.remme.tinygit.system.git.Remote
import hamburg.remme.tinygit.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify

@DisplayName("Testing repository service")
@ExtendWith(MockitoExtension::class)
internal class RepositoryServiceTest {

    @Mock lateinit var log: Log
    @Mock lateinit var remote: Remote
    private lateinit var service: RepositoryService

    private val result = listOf(Commit("12345678"))

    @BeforeEach fun setup() {
        whenever(log.query(CURRENT_DIR)).thenReturn(result)
        service = RepositoryService(log, remote)
    }

    @DisplayName("Testing list")
    @Test fun testList() {
        // When
        val list = service.list(CURRENT_DIR)

        // Then
        verify(log).query(CURRENT_DIR)
        assertThat(list).isEqualTo(result)
    }

    @DisplayName("Testing count")
    @Test fun testCount() {
        // When
        val count = service.count(CURRENT_DIR)

        // Then
        verify(log).query(CURRENT_DIR)
        assertThat(count).isEqualTo(result.size)
    }

    @DisplayName("Testing update")
    @Test fun testUpdate() {
        // When
        service.update(CURRENT_DIR)

        // Then
        verify(remote).pull(CURRENT_DIR)
    }

}
