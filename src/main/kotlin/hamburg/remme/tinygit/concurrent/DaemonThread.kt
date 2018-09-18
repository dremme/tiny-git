package hamburg.remme.tinygit.concurrent

/**
 * A thread that is always a daemon.
 */
class DaemonThread(runnable: Runnable) : Thread(runnable) {

    init {
        isDaemon = true
    }

}
