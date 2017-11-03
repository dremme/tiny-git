package hamburg.remme.tinygit.git

import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.RemoteRefUpdate

class PushRejectedException : TransportException(RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD.name)
