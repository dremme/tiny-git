package hamburg.remme.tinygit.system.git

/**
 * The commit id that relates to the `/dev/null` tree.
 */
internal const val EMPTY_ID: String = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"
/**
 * Shows the installed version of the Git client.
 */
internal const val VERSION: String = "version"
/**
 * Lists commit objects in reverse chronological order: https://git-scm.com/docs/git-rev-list
 */
internal const val REV_LIST: String = "rev-list"
/**
 * Show commit logs: https://git-scm.com/docs/git-log
 */
internal const val LOG: String = "log"
/**
 * Pulls new commits and resets the HEAD to the latest commit: https://git-scm.com/docs/git-pull
 */
internal const val PULL: String = "pull"
/**
 * A log pattern to use with `--pretty` arguments. This will print commit info in the following order:
 * commit ID, short commit ID, parent IDs, short parent IDs, author mail, author name, author date (UNIX),
 * committer mail, committer name, committer date (UNIX), subject
 */
internal const val LOG_PATTERN: String = "%H%n%h%n%P%n%p%n%ae%n%an%n%at%n%ce%n%cn%n%ct%n%s"
/**
 * The number of log pattern lines until a commit info is completely printed.
 *
 * `LOG_PATTERN.split("%n") - 1`
 */
internal const val LOG_PATTERN_LINES: Int = 11
