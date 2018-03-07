package hamburg.remme.tinygit.domain

import java.time.LocalDate

class CommitIsh(id: String) : Commit(id, emptyList(), "", LocalDate.of(1900, 1, 1).atStartOfDay(), "-", "-")
