package hamburg.remme.tinygit.git

import org.eclipse.jgit.api.errors.GitAPIException

class TimeoutException : RuntimeException()

class PullException(message: String) : RuntimeException(message)

class CheckoutException : RuntimeException()

class PushException : RuntimeException()

class PrepareSquashException(message: String) : GitAPIException(message)

class SquashException(message: String) : GitAPIException(message)

class StashPopException : RuntimeException()