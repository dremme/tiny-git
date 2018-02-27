package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.asPath
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.exists
import hamburg.remme.tinygit.git.gitClone
import hamburg.remme.tinygit.git.gitGc
import hamburg.remme.tinygit.git.gitGetUrl
import hamburg.remme.tinygit.git.gitInit
import hamburg.remme.tinygit.json
import hamburg.remme.tinygit.observableList
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.concurrent.Task

class RepositoryService(private val service: CredentialService) {

    private val allRepositories = observableList<Repository>()
    val existingRepositories = allRepositories.filtered { it.path.asPath().exists() }!!
    val activeRepository = object : SimpleObjectProperty<Repository?>() {
        override fun invalidated() = get()?.let { remote.set(gitGetUrl(it)) } ?: remote.set("")
    }
    val remote = SimpleStringProperty()
    val hasRemote = remote.isNotEmpty!!
    val usedNames = observableList<String>()
    val usedEmails = observableList<String>()
    val usedProxies = observableList<String>()

    init {
        TinyGit.settings.addOnSave {
            it["repositories"] = allRepositories.map { json { +("path" to it.path) } }
            it["usedNames"] = usedNames
            it["usedEmails"] = usedEmails
            it["usedProxies"] = usedProxies
        }
        TinyGit.settings.load {
            it.getList("repositories")?.map { Repository(it.getString("path")!!) }?.let { allRepositories.setAll(it) }
            it.getStringList("usedNames")?.let { usedNames.setAll(it) }
            it.getStringList("usedEmails")?.let { usedEmails.setAll(it) }
            it.getStringList("usedProxies")?.let { usedProxies.setAll(it) }
        }
    }

    fun init(path: String) {
        gitInit(path)
        add(Repository(path))
    }

    fun clone(repository: Repository, url: String, proxy: String,
              successHandler: () -> Unit,
              errorHandler: (String) -> Unit) {
        service.applyCredentials(url)
        TinyGit.execute("Cloning...", object : Task<Unit>() {
            override fun call() = gitClone(repository, proxy, url)

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

    fun addUsedName(name: String) {
        if (!usedNames.contains(name)) usedNames += name
    }

    fun addUsedEmail(email: String) {
        if (!usedEmails.contains(email)) usedEmails += email
    }

    fun addUsedProxy(proxy: String) {
        if (!usedProxies.contains(proxy)) usedProxies += proxy
    }

    private fun add(repository: Repository) {
        if (!allRepositories.contains(repository)) allRepositories += repository
    }

}
