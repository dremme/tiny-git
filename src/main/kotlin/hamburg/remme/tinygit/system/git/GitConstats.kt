package hamburg.remme.tinygit.system.git

/**
 * Shows the installed version of the Git client.
 */
internal const val VERSION = "version"
/**
 * Lists commit objects in reverse chronological order: https://git-scm.com/docs/git-rev-list
 */
internal const val REV_LIST = "rev-list"
/**
 * Show commit logs: https://git-scm.com/docs/git-log
 */
internal const val LOG = "log"
/**
 * A log pattern to use with `--pretty` arguments. This will print commit info in the following order:
 * commit ID, commit short ID, parent IDs, parent short IDs, author mail, author name, author date (UNIX),
 * committer mail, committer name, committer date (UNIX)
 */
internal const val LOG_PATTERN = "%H%n%h%n%P%n%p%n%ae%n%an%n%at%n%ce%n%cn%n%ct"
/**
 * The number of log pattern lines until a commit info is completely printed.
 *
 * `LOG_PATTERN.split("%n") - 1`
 */
internal const val LOG_PATTERN_LINES = 10
