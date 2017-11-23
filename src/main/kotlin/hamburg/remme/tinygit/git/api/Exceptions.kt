package hamburg.remme.tinygit.git.api

import org.eclipse.jgit.errors.TransportException

class PushRejectedException(message: String) : TransportException(message)

class DeleteRejectedException(message: String) : TransportException(message)

class RemoteChangedException(message: String) : TransportException(message)

class RejectedException(message: String) : TransportException(message)
