package hamburg.remme.tinygit.domain

class GitStatus(val staged: List<GitFile>, val pending: List<GitFile>)
