package hamburg.remme.tinygit.domain.service

import javafx.concurrent.Task

interface TaskExecutor {

    fun execute(task: Task<*>)

}
