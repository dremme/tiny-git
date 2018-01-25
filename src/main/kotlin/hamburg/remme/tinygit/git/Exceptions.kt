package hamburg.remme.tinygit.git

class TimeoutException : RuntimeException()

class CloneException(message: String) : RuntimeException(message)

class PullException(message: String) : RuntimeException(message)

class CheckoutException : RuntimeException()

class BranchAlreadyExistsException : RuntimeException()

class BranchNameInvalidException : RuntimeException()

class BranchUnpushedException : RuntimeException()

class BranchBehindException : RuntimeException()

class RebaseException(message: String) : RuntimeException(message)

class UnmergedException : RuntimeException()

class StashPopException : RuntimeException()
