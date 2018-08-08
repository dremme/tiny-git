package hamburg.remme.tinygit.system.git

import java.time.Instant
import kotlin.reflect.KClass

/**
 * The fields are named after the placeholders in [LOG_PATTERN].
 */
@Suppress("EnumEntryName")
enum class CommitProperty(val type: KClass<*>) {

    /**
     * Commit ID (hash).
     */
    H(String::class),
    /**
     * Short commit ID (hash).
     */
    h(String::class),
    /**
     * Parent commit IDs (hashes).
     */
    P(List::class),
    /**
     * Parent short commit IDs (hashes).
     */
    p(List::class),
    /**
     * Author mail.
     */
    ae(String::class),
    /**
     * Author name.
     */
    an(String::class),
    /**
     * Author date.
     */
    at(Instant::class),
    /**
     * Committer mail.
     */
    ce(String::class),
    /**
     * Committer name.
     */
    cn(String::class),
    /**
     * Committer date.
     */
    ct(Instant::class),

}
