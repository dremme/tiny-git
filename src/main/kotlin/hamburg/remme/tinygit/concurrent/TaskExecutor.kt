package hamburg.remme.tinygit.concurrent

import javafx.concurrent.Task

/**
 * An executor for wrapping [Task]s.
 */
interface TaskExecutor {

    /**
     * Executes [call] then invokes [succeeded] with the result of [call] or `null` if none. Any exceptions will cause
     * [failed] to be invoked with no result.
     * @param call      should return a result of type [T] or [Unit].
     * @param succeeded receives the result of type [T] or `null` if there is no result.
     * @param failed    receives the exception that caused the failure.
     */
    fun <T> submit(call: () -> T, succeeded: (T?) -> Unit = {}, failed: (Throwable) -> Unit = {})

}
