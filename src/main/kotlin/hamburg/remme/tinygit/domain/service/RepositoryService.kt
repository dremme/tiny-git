package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.gitClone
import hamburg.remme.tinygit.git.gitGc
import hamburg.remme.tinygit.git.gitGetProxy
import hamburg.remme.tinygit.git.gitHasRemote
import hamburg.remme.tinygit.git.gitInit
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task

class RepositoryService {

    private val allRepositories = observableList<Repository>()
    val existingRepositories = allRepositories.filtered { it.path.asPath().exists() }!!
    val activeRepository = SimpleObjectProperty<Repository?>()
    val hasRemote = SimpleBooleanProperty()

    init {
        activeRepository.addListener { _, _, it -> it?.let { hasRemote.set(gitHasRemote(it)) } ?: hasRemote.set(false) }
        TinyGit.settings.setRepositories { allRepositories }
        TinyGit.settings.load { allRepositories.setAll(it.repositories) }
    }

    fun init(path: String) {
        gitInit(path)
        add(Repository(path))
    }

    fun clone(repository: Repository, url: String, errorHandler: (String) -> Unit) {
        TinyGit.execute("Cloning...", object : Task<Unit>() {
            override fun call() = gitClone(repository, url)

            override fun succeeded() = add(repository)

            override fun failed() = errorHandler.invoke(exception.message!!)
        })
    }

    fun open(path: String, invalidHandler: () -> Unit) {
        if (path.asPath().resolve(".git").exists()) {
            val repository = Repository(path)
            val match = "(.+):(\\d+)".toRegex().matchEntire(gitGetProxy(repository).trim())!!.groups
            repository.proxyHost = match[1]?.value ?: ""
            repository.proxyPort = match[2]?.value?.toInt() ?: 80
            add(repository)
        } else {
            invalidHandler.invoke()
        }
    }

    fun remove(repository: Repository) = allRepositories.remove(repository)

    fun gc() {
        TinyGit.execute("Cleaning up...", object : Task<Unit>() {
            override fun call() = gitGc(activeRepository.get()!!)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    private fun add(repository: Repository) {
        if (!allRepositories.contains(repository)) allRepositories.add(repository)
    }

}
