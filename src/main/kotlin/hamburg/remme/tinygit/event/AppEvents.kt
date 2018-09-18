package hamburg.remme.tinygit.event

/**
 * Displays a progress overlay. Multiple [ShowProgressEvent] will build up a stack, that has to be popped by sending
 * [HideProgressEvent]s.
 */
class ShowProgressEvent : Event()

/**
 * Pops the latest [ShowProgressEvent] from the progress overlay stack. If the stack is empty no overlay is being shown.
 */
class HideProgressEvent : Event()

/**
 * When the application should be shutdown.
 * This will trigger the whole shutdown procedure for JavaFX and Spring.
 */
class QuitEvent : Event()
