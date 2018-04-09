package hamburg.remme.tinygit

import java.util.jar.JarFile

/**
 * The users home folder.
 */
val homeDir = System.getProperty("user.home")!!
private val operatingSystem = System.getProperty("os.name")
/**
 * The running OS is Windows.
 */
val isWindows = operatingSystem.startsWith("Windows")
/**
 * The running OS is Mac OS.
 */
val isMac = operatingSystem.startsWith("Mac")
/**
 * The running OS is some kind of Linux or UNIX. This could also be embedded or Android.
 */
val isLinux = operatingSystem.startsWith("Linux")
/**
 * The OS default font size.
 *
 * @todo: maybe has to be initialized for high DPI Linux in a separate way.
 */
val fontSize = if (isMac) 13 else 12

/**
 * `true` if the application is executing from within a JAR file.
 */
fun isJar() = TinyGit::class.java.getResource("${TinyGit::class.java.simpleName}Kt.class").toExternalForm().startsWith("jar")

/**
 * The running JAR file or bullshit if the code isn't executed from within a JAR.
 *
 * @see isJar
 */
fun jarFile() = JarFile(TinyGit::class.java.protectionDomain.codeSource.location.toURI().asFile())
