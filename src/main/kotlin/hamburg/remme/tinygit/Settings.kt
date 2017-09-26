package hamburg.remme.tinygit

import hamburg.remme.tinygit.git.LocalRepository
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Files

object Settings {

    private val yaml = Yaml()
    private val settings = File("${System.getProperty("user.home")}/.tinygit").toPath()

    @Suppress("UNCHECKED_CAST")
    fun load(): List<LocalRepository> {
        if (Files.exists(settings)) {
            try {
                return (yaml.load(Files.newInputStream(settings)) as LocalSettings).repositories
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return listOf()
    }

    fun save(repositories: List<LocalRepository>) {
        Files.write(settings, yaml.dump(LocalSettings(repositories)).toByteArray())
    }

    class LocalSettings(var repositories: List<LocalRepository> = listOf())

}
