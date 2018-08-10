package hamburg.remme.tinygit.system.git

import java.time.Instant
import kotlin.reflect.KClass

/**
 * The fields are named after the placeholders in [LOG_PATTERN].
 */
@Suppress("EnumEntryName")
enum class CommitProperty(val type: KClass<*>, val pattern: String) {

    /**
     * Commit ID (hash).
     */
    H(String::class, "H"),
    /**
     * Short commit ID (hash).
     */
    h(String::class, "h"),
    /**
     * Parent commit IDs (hashes).
     */
    P(List::class, "P"),
    /**
     * Parent short commit IDs (hashes).
     */
    p(List::class, "p"),
    /**
     * Author mail.
     */
    ae(String::class, "ae"),
    /**
     * Author name.
     */
    an(String::class, "an"),
    /**
     * Author date.
     */
    at(Instant::class, "at"),
    /**
     * Committer mail.
     */
    ce(String::class, "ce"),
    /**
     * Committer name.
     */
    cn(String::class, "cn"),
    /**
     * Committer date.
     */
    ct(Instant::class, "ct"),

}
