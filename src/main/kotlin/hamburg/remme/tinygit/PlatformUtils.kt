package hamburg.remme.tinygit

/**
 * The users home folder.
 */
val homeDir = System.getProperty("user.home")!!
private val os = System.getProperty("os.name")
private val version = System.getProperty("os.version")
/**
 * The running OS is Windows.
 */
val isWindows = os.startsWith("Windows")
/**
 * The running OS is Mac OS.
 */
val isMac = os.startsWith("Mac")
/**
 * The running OS is some kind of Linux or UNIX. This could also be embedded or Android.
 */
val isLinux = os.startsWith("Linux")
/**
 * The OS default font size.
 *
 * @todo: maybe has to be initialized for high DPI Linux in a separate way.
 */
val fontSize = if (isMac) 13 else 12
