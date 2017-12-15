package hamburg.remme.tinygit.git.api

import org.eclipse.jgit.api.errors.GitAPIException

class PushRejectedException(message: String) : GitAPIException(message)

class DeleteRejectedException(message: String) : GitAPIException(message)

class RemoteChangedException(message: String) : GitAPIException(message)

class RejectedException(message: String) : GitAPIException(message)

class PrepareSquashException(message: String) : GitAPIException(message)

class SquashException(message: String) : GitAPIException(message)
