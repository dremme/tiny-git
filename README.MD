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

 - Creating and cloning Git repositories
 - Separate settings for each repository
 - Working with stashes
 - Working with branches
 - Automatic squashing of feature branches
 - Automatic reset if a branch diverged from remote branch

### Missing Features

 - Merging branches
 - Tracking only wanted branches
 - Commit graph in the log
 - Viewing/applying/removing explicit stashes
 - Resetting branches
 - Tagging commits & push/remove tags
 - Staging/unstaging code blocks
 - Saving selected tab
 - Opening selected file in explorer/finder
 - Pull request notifications?
 - Set author and email per repository
