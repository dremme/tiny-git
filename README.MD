## TinyGit
A very small, fast and portable Git client.
Repositories can be configured with credentials, SSH keys and proxies separately.
No need to toggle the proxy setting of `.gitconfig`.

![TinyGit](image/image1.png)

### Usage

TinyGrid runs without a Git installation on your device.

Requires Java 1.8 or newer to be installed.

```bash
java -jar tinygit-x.x.x.jar
```

### Features

 - Staging/unstaging files
 - Adding local Git repositories
 - Separate settings for each repository
 - Creating/popping stashes
 - Fetching/pulling/pushing commits
 - Branching
 - Commit log and detailed file diffs

### Missing Features

 - Squashing branches (interactive rebase)
 - Rebasing and merging branches
 - Creating/cloning/removing repositories
 - Removing branches
 - Changing local repo/remote URL
 - Tracking only wanted branches
 - Commit graph in the log
 - Endless scroll the log
 - Viewing/applying/removing explicit stashes
 - Resetting branches
 - Tagging commits & push/remove tags
 - File diff settings (e.g. surrounding lines)
 - Staging/unstaging code blocks
 - Saving selected tab
 - Saving window state
 - Opening selected file in explorer/finder
 - Pull request notifications?
 - Set author and email per repository
