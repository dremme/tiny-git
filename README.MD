## TinyGit
A very small, fast and portable Git client.
Repositories can be configured with credentials, SSH keys and proxies separately.
No need to toggle the proxy setting of `.gitconfig`.

![TinyGit](image/image1.png)

### Usage

TinyGrid doesn't need a Git installation on your device.

Requires Java 1.8 or newer to be installed.

```bash
java -jar tinygit-x.x.x.jar
```

### Missing Features

 - Squashing branches (interactive rebase)
 - Rebasing and merging branches
 - Creating/cloning/removing repositories
 - Changing remote URL
 - Track only wanted branches
 - Commit graph in the log
 - Endless scroll the log
 - Apply/remote explicit stashes
 - Resetting branches
 - Tag commits
 - Push/remove tags
 - File diff settings (e.g. surrounding lines)
 - Stage/unstage code blocks
