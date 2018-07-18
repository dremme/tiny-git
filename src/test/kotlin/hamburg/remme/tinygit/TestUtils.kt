package hamburg.remme.tinygit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.mockito.Mock
import org.mockito.MockitoAnnotations

/**
 * A basic extension to get the Mockito [Mock] annotation working again.
 */
class MockitoExtension : TestInstancePostProcessor {

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockitoAnnotations.initMocks(testInstance)
    }

}
