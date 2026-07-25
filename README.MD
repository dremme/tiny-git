# TinyGit

**A small, fast, portable Git desktop client.**

Work with multiple repositories, each with its own credentials, SSH key, and proxy.
No more flipping global `.gitconfig` settings when you switch between networks or accounts.

TinyGit shells out to your local [Git](https://git-scm.com/) installation and focuses on day-to-day history, working-copy, and branch workflows—plus repository statistics with a selectable time range.

**Version:** 0.10.0-SNAPSHOT · **License:** [BSD 3-Clause](LICENSE)

[Features](#features) · [Requirements](#requirements) · [Quick start](#quick-start) · [Project layout](#project-layout) · [Development](#development) · [Architecture](#architecture) · [License](#license)

---

## Why TinyGit?

Many Git GUIs either feel heavy or assume one global Git configuration for everything.
TinyGit is built around a few deliberate choices:

- **Per-repository settings** for remote URL, credentials, SSH keys, and HTTP proxy
- **Native Git CLI** under the hood—behavior matches the tools you already trust
- **Portable desktop UI** (Kotlin + JavaFX) on Windows, macOS, and Linux
- **Focused surface area**—commit log with graph, working copy, branches/tags/stash, and stats

Ideal when you juggle several remotes or proxies and want a client rather than a full IDE.

---

## Features

### Repositories

- Open existing local repositories
- Clone from a remote URL
- Initialize a new repository
- Keep several repos in a sidebar list and switch quickly
- Remove a repository from the list without deleting files on disk
- **Per-repo settings:** remote, username/password or SSH key, proxy

### Commit log

- Scrollable history with a **branch graph** overlaid on the list
- Commit details (hash, parents, author, date, message)
- File list for a commit and HTML-rendered diffs
- Filter: all branches vs current branch only
- Filter: all commits vs no merges
- Search the log
- Checkout a commit, reset to a commit, or create a tag
- Background fetch with **prune** (stale remote-tracking branches such as deleted `origin/master` are removed)

### Working copy

- Staged and pending (unstaged) file lists
- Stage / unstage selected or all
- Update all, discard changes, delete files
- Commit dialog
- Diff view for selected paths (context-line count control)
- Status counts by change type (added, modified, removed, …)

### Branches, merge, rebase

- Local and remote branch trees
- Checkout, create, rename, delete branches
- Merge with continue / abort
- Rebase with continue / abort
- **Auto-Reset** when the branch has diverged from its remote
- **Auto-Squash** for feature-branch cleanup
- Default-branch awareness includes **`main`** and **`master`** (and common long-lived names)

### Stash and tags

- Stash and pop stash
- Apply or delete stash entries from the repository tree
- List tags; create and delete tags

### Remotes and maintenance

- Fetch (with prune), pull, push, force push
- Garbage collection (`git gc`)
- Optional raw **Git command** dialog for power users

### Statistics

Analytics over a selectable time range:

| Control / chart | Meaning |
| --- | --- |
| **Time range** | **Last 12 months** (rolling) or a **calendar year** discovered from history |
| **Top contributors** | Commits grouped by author email |
| **Common file types** | File extensions from tree stats |
| **Daily activity** | Calendar-style commit intensity |
| **Contributions** | Commits over time by author |
| **Lines of code** | Added / removed lines by week |

Refresh stats from the toolbar when needed.

### Desktop UX

- Dark Material-inspired theme (**Inter** typeface + **Font Awesome 7** icons)
- Platform stylesheets (Windows 12px / macOS 13px base metrics)
- Gray text anti-aliasing tuned for the dark UI
- Native menu bar integration on macOS (NSMenuFX)
- English and German message bundles
- Window size and position persistence under `~/.tinygit`
- First-run and restored window size clamped to the primary screen’s visual bounds

---

## Requirements

| Dependency | Notes |
| --- | --- |
| **Git 2.x+** | Must be on the `PATH`. TinyGit refuses to start without a usable client. |
| **JDK 21** | LTS. Set `JAVA_HOME` (or install a JDK 21 for Gradle toolchains). |
| **OS** | Windows, macOS, or Linux (desktop) |

Optional for development:

| Tool | When you need it |
| --- | --- |
| [Dart Sass](https://sass-lang.com/install) or Node/`npx` | Recompile SCSS → CSS. Seed CSS under `scss-seed/` is used if Sass is missing. |

Install Git:

- **Windows:** [git-scm.com/downloads](https://git-scm.com/downloads)
- **macOS:** `brew install git`
- **Linux:** distro package (e.g. `sudo apt install git`)

---

## Quick start

### Run from source (recommended)

Requires **JDK 21** and **Git**.
The Gradle wrapper downloads the rest.

```bash
# macOS / Linux
chmod +x gradlew
./gradlew run

# Windows
gradlew.bat run
```

`run` wires the OpenJFX module path for you.

### Build artifacts

```bash
./gradlew clean assemble
```

Useful outputs:

| Output | Path / task |
| --- | --- |
| Application distribution | `./gradlew installDist` → `build/install/tiny-git/` |
| Fat JAR (app + non-JavaFX deps) | `build/libs/tiny-git-*-all.jar` |
| Start scripts | under `build/` after `assemble` / `installDist` |

Launch the installed distribution:

```bash
# Unix
./build/install/tiny-git/bin/tiny-git

# Windows
build\install\tiny-git\bin\tiny-git.bat
```

### First launch checklist

1. Confirm `git --version` works in a terminal.
2. Start TinyGit (`./gradlew run`).
3. **Add**, **Clone**, or **New** a repository from the empty state or File menu.
4. Use **Commit Log**, **Working Copy**, and **Statistics** tabs as needed.
5. Open **Settings** on a selected repo for remote, credentials, and proxy.

Settings are stored in a YAML file at `~/.tinygit` (user home).

---

## Project layout

```text
TinyGit/
├── build.gradle              # Kotlin 2.1, Java 21, OpenJFX 21, Spotless, Sass task
├── settings.gradle           # Root project name
├── gradle.properties         # JVM / Gradle defaults
├── gradle/wrapper/           # Gradle 8.12 wrapper
├── scss-seed/                # Fallback CSS if Sass is unavailable
├── src/main/
│   ├── kotlin/hamburg/remme/tinygit/
│   │   ├── TinyGit.kt        # Application entry, DI bootstrap
│   │   ├── AppFonts.kt       # Embedded fonts + Prism text settings
│   │   ├── domain/           # Models + services (log, branch, stats, …)
│   │   ├── git/              # Thin wrappers around the Git CLI
│   │   └── gui/              # JavaFX views, dialogs, custom charts/skins
│   └── resources/hamburg/remme/tinygit/
│       ├── *.scss            # Theme (compiled to CSS at build time)
│       ├── font/             # Inter + Font Awesome 7; Consolas for mono (system)
│       └── message*.properties
├── AGENTS.md                 # Conventions for coding agents
└── LICENSE                   # BSD 3-Clause
```

Generated styles land in `build/generated/resources/css/` (classpath `/css/…`).

---

## Development

### Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin **2.1.x** |
| Runtime | Java **21** LTS |
| UI | OpenJFX **21** (controls, web, media) |
| Build | Gradle **8.12** (wrapper) |
| Formatting | Spotless + ktlint **1.5** |
| Styling | SCSS → JavaFX CSS (Dart Sass) |
| Config | SnakeYAML → `~/.tinygit` |
| macOS menus | CenterDevice NSMenuFX |

### Common tasks

| Task | Command |
| --- | --- |
| Run app | `./gradlew run` |
| Compile only | `./gradlew compileKotlin` |
| Recompile styles | `./gradlew compileSCSS` |
| Full assemble | `./gradlew clean assemble` |
| Format check | `./gradlew spotlessCheck` |
| Apply formatting | `./gradlew spotlessApply` |
| Full check (includes Spotless) | `./gradlew check` |
| Print JavaFX run hint | `./gradlew printRunCommand` |

### Stylesheets

- Sources: `src/main/resources/hamburg/remme/tinygit/*.scss`
- Entry points: `main-windows.scss` (12px base), `main-mac.scss` (13px base)
- Output: `build/generated/resources/css/main-*.css` (classpath `/css/…`)
- Fallback seeds: `scss-seed/main-*.css` when Sass cannot run
- The build sanitizes Dart Sass colors (`hsl()`, percent `rgb()`) into forms JavaFX accepts
- JavaFX `null` paints are emitted via `$fx-null` so combo-box / icon layers stay transparent

### Git integration notes

- All Git operations go through process execution (`git …`), not JGit
- Empty placeholder tree id `4b825dc…` is used where Git expects the empty tree
- Credential helper is nudged to `wincred` (Windows) or Keychain (macOS) when unset
- Fetch uses `--prune` so deleted remote branches disappear from the UI after refresh
- Global Git still must be installed and discoverable on `PATH`

There is **no** hot reload for `./gradlew run` (desktop JVM + JavaFX).
Use continuous compile (`./gradlew -t classes`) plus restart if you want a faster edit loop.

---

## Architecture

TinyGit is a classic desktop layout:

```text
┌─────────────────────────────────────────────────────────┐
│  TinyGit (JavaFX Application)                           │
│    · scans @Service types, constructor-injects graph    │
│    · owns Stage, settings lifecycle, global tasks       │
└───────────────────────────┬─────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
   gui/* views         domain/service/*      git/* CLI
   (JavaFX)            (state + use cases)   (ProcessBuilder)
```

| Package | Role |
| --- | --- |
| `gui` | Tabs (log, working copy, stats), dialogs, custom list skin for the graph, donut/histogram/calendar charts |
| `domain` | Repository/commit/branch models and services that orchestrate Git calls on background tasks |
| `git` | Stringly-typed wrappers around `git log`, `diff`, `branch`, `merge`, etc. |

HTML diffs and commit details are rendered in a **WebView**.
The commit graph is drawn by a custom `ListView` skin on top of the standard virtual flow.

---

## Configuration and data

| Item | Location |
| --- | --- |
| App settings (window, repo list, per-repo options) | `~/.tinygit` (YAML) |
| Git config / credentials | Your normal Git / OS credential helper setup |
| No cloud account | Everything stays local |

Proxy and SSH paths can be set **per repository** in Settings so different remotes do not fight over a single global Git config.

---

## Contributing

Issues and pull requests are welcome at [github.com/dremme/tiny-git](https://github.com/dremme/tiny-git).

When changing UI or styles, verify:

- Commit log graph scrolling and selection
- Working copy stage/commit flows
- Diff WebView readability
- Statistics for last-12-months and calendar-year ranges
- Combo boxes and toolbar icons (font faces + anti-aliasing)
- Window size on a small screen (first launch and restore)

Format with Spotless before opening a PR:

```bash
./gradlew spotlessApply check
```

Agent-oriented project conventions live in [`AGENTS.md`](AGENTS.md).

---

## License

Licensed under the [BSD 3-Clause License](LICENSE).

Copyright (c) 2017–2026, Dennis Remme.

---

Feedback and contributions are welcome.
Open an issue or pull request if something is missing or broken.
