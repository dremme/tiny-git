package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.gitClone
import hamburg.remme.tinygit.git.gitGc
import hamburg.remme.tinygit.git.gitHasRemote
import hamburg.remme.tinygit.git.gitInit
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.concurrent.Task

class RepositoryService {

    private val allRepositories = observableList<Repository>()
    val existingRepositories = allRepositories.filtered { it.path.asPath().exists() }!!
    val activeRepository = object : SimpleObjectProperty<Repository?>() {
        override fun invalidated() {
            get()?.let { hasRemote.set(gitHasRemote(it)) } ?: hasRemote.set(false)
        }
    }
    val hasRemote = SimpleBooleanProperty()

    init {
        TinyGit.settings.setRepositories { allRepositories }
        TinyGit.settings.load { allRepositories.setAll(it.repositories) }
    }

    fun init(path: String) {
        gitInit(path)
        add(Repository(path))
    }

    fun clone(repository: Repository, url: String, proxyHost: String, proxyPort: Int,
              successHandler: () -> Unit, errorHandler: (String) -> Unit) {
        TinyGit.execute("Cloning...", object : Task<Unit>() {
            override fun call() = gitClone(repository, proxyHost, proxyPort, url)

            override fun succeeded() {
                add(repository)
                successHandler.invoke()
            }

            override fun failed() = errorHandler.invoke(exception.message!!)
        })
    }

    fun open(path: String, invalidHandler: () -> Unit) {
        if (path.asPath().resolve(".git").exists()) {
            add(Repository(path))
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
