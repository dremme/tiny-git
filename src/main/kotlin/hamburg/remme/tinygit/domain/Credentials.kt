package hamburg.remme.tinygit.domain

class Credentials(val username: String, val password: String, val host: String = "", val protocol: String = "") {

    val isEmpty = username.isBlank()

}
