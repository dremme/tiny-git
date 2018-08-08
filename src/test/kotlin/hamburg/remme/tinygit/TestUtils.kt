package hamburg.remme.tinygit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.stubbing.OngoingStubbing

/**
 * A regex pattern for commit IDs.
 */
internal const val COMMIT_ID_PATTERN: String = "[a-f0-9]{40}"
/**
 * A regex pattern for short commit IDs.
 */
internal const val SHORT_COMMIT_ID_PATTERN: String = "[a-f0-9]{7}"
/**
 * A regex pattern for emails.
 */
internal const val MAIL_PATTERN: String = ".+@.+\\..+"

/**
 * Delegates to [Mockito.when].
 */
internal fun <T> whenever(method: T): OngoingStubbing<T> = Mockito.`when`(method)

/**
 * A basic extension to get the Mockito [Mock] annotation working again.
 */
internal class MockitoExtension : TestInstancePostProcessor {

    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        MockitoAnnotations.initMocks(testInstance)
    }

}
