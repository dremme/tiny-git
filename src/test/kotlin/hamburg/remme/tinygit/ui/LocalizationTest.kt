package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.UTF8Support
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Locale
import java.util.ResourceBundle

@DisplayName("Testing localization")
class LocalizationTest {

    @BeforeEach fun setup() {
        Locale.setDefault(Locale.ROOT)
    }

    @DisplayName("Testing resource bundle")
    @CsvSource("symbol1,megawatt (MW)", "symbol2,microsecond (µs)")
    @ParameterizedTest fun testResourceBundle(key: String, value: String) {
        // Given
        val basename = "test_messages"

        // When
        val bundle = ResourceBundle.getBundle(basename, UTF8Support())

        // Then
        assertThat(bundle.getString(key)).isEqualTo(value)
    }

    @DisplayName("Testing different locale")
    @Test fun testLocale() {
        // Given
        Locale.setDefault(Locale.GERMANY)
        val basename = "test_messages"
        val key = "symbol2"
        val value = "Mikrosekunde (µs)"

        // When
        val bundle = ResourceBundle.getBundle(basename, UTF8Support())

        // Then
        assertThat(bundle.getString(key)).isEqualTo(value)
    }

}
