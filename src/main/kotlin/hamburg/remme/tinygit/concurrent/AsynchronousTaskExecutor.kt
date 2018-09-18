package hamburg.remme.tinygit.concurrent

import javafx.concurrent.Task
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Executes the blocks asynchronously in a [Task].
 */
@Component class AsynchronousTaskExecutor : TaskExecutor {

    private val service: ExecutorService = Executors.newSingleThreadExecutor(::DaemonThread) // FIXME: more threads

    /**
     * Executes [call] asynchronously then invokes [succeeded] with the result of [call] in the JavaFX thread or `null`
     * if none. Any exceptions will cause [failed] to be invoked in the JavaFX thread with no result.
     * @param call      see [Task.call]. Should return a result of type [T] or [Unit].
     * @param succeeded see [Task.succeeded]. Receives the result of type [T] or `null` if there is no result.
     * @param failed    see [Task.failed]. Receives the exception that caused the failure.
     */
    override fun <T> submit(call: () -> T, succeeded: (T?) -> Unit, failed: (Throwable) -> Unit) {
        service.submit(object : Task<T>() {
            override fun call(): T = call()
            override fun succeeded() = succeeded(value)
            override fun failed() = failed(exception)
        })
    }

}
