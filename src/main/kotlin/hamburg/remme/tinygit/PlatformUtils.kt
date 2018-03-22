package hamburg.remme.tinygit

val homeDir = System.getProperty("user.home")!!
private val os = System.getProperty("os.name")
private val version = System.getProperty("os.version")
val isWindows = os.startsWith("Windows")
val isMac = os.startsWith("Mac")
val isLinux = os.startsWith("Linux")
