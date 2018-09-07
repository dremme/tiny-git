package hamburg.remme.tinygit.system.git

import java.time.Instant

/**
 * An object representing a Git commit.
 * @property id             Commit ID (hash).
 * @property shortId        Short commit ID (hash).
 * @property parents        Parent commit IDs (hashes).
 * @property shortParents   Parent short commit IDs (hashes).
 * @property authorEmail    Author email.
 * @property authorName     Author name.
 * @property authorTime     Author commit time.
 * @property committerEmail Committer email.
 * @property committerName  Committer name.
 * @property committerTime  Committer commit time.
 * @property message        Commit message (might be only the commit subject).
 */
class Commit(val id: String = EMPTY_ID,
             val shortId: String = EMPTY_ID,
             val parents: List<String> = emptyList(),
             val shortParents: List<String> = emptyList(),
             val authorEmail: String = "",
             val authorName: String = "",
             val authorTime: Instant = Instant.now(),
             val committerEmail: String = "",
             val committerName: String = "",
             val committerTime: Instant = Instant.now(),
             val message: String = "") {

    override fun toString(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Commit

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
