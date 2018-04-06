package hamburg.remme.tinygit.domain.service

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.Refreshable
import hamburg.remme.tinygit.Service
import hamburg.remme.tinygit.TinyGit
import hamburg.remme.tinygit.domain.Commit
import hamburg.remme.tinygit.domain.Repository
import hamburg.remme.tinygit.domain.Tag
import hamburg.remme.tinygit.execute
import hamburg.remme.tinygit.git.TagAlreadyExistsException
import hamburg.remme.tinygit.git.gitTag
import hamburg.remme.tinygit.git.gitTagDelete
import hamburg.remme.tinygit.git.gitTagList
import hamburg.remme.tinygit.observableList
import javafx.concurrent.Task

@Service
class TagService(private val repositoryService: RepositoryService,
                 private val credentialService: CredentialService) : Refreshable {

    val tags = observableList<Tag>()
    private lateinit var repository: Repository
    private var task: Task<*>? = null

    fun tag(commit: Commit, name: String, errorHandler: () -> Unit) {
        credentialService.applyCredentials(repositoryService.remote.get())
        TinyGit.run(I18N["tag.tagging"], object : Task<Unit>() {
            override fun call() = gitTag(repository, commit, name)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() {
                when (exception) {
                    is TagAlreadyExistsException -> errorHandler()
                    else -> exception.printStackTrace()
                }
            }
        })
    }

    fun delete(tag: Tag) {
        credentialService.applyCredentials(repositoryService.remote.get())
        TinyGit.run(I18N["tag.deleting"], object : Task<Unit>() {
            override fun call() = gitTagDelete(repository, tag)

            override fun succeeded() = TinyGit.fireEvent()

            override fun failed() = exception.printStackTrace()
        })
    }

    override fun onRefresh(repository: Repository) {
        update(repository)
    }

    override fun onRepositoryChanged(repository: Repository) {
        onRepositoryDeselected()
        update(repository)
    }

    override fun onRepositoryDeselected() {
        task?.cancel()
        tags.clear()
    }

    private fun update(repository: Repository) {
        this.repository = repository
        task?.cancel()
        task = object : Task<List<Tag>>() {
            override fun call() = gitTagList(repository)

            override fun succeeded() {
                tags.setAll(value)
            }
        }.execute()
    }

}
