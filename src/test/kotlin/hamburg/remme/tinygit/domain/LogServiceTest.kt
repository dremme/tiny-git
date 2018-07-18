package hamburg.remme.tinygit.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class LogServiceTest {

    lateinit var service: LogService

    @BeforeEach
    fun setup() {
        service = LogService()
    }

    @Test
    fun testCountAll() {
        // When
        val result = service.countAll()

        // Then
        assertThat(result).isGreaterThan(1)
    }

}
