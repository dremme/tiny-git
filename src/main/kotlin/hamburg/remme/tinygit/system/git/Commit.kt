package hamburg.remme.tinygit.system.git

import java.time.Instant

data class Commit(val id: String,
                  val shortId: String,
                  val parentIds: List<String>,
                  val parentShortIds: List<String>,
                  val authorMail: String,
                  val authorName: String,
                  val authorDate: Instant,
                  val committerMail: String,
                  val committerName: String,
                  val committerDate: Instant)
