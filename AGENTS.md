# Agent instructions

These are common instructions for agents across all scenarios.

## General Guidelines

- Never use '…', even in user-facing code; always use '...'.
- Never use lewd, explicit, or NSFW terms in test fixtures, examples, placeholders, or production default values in the frontend or backend.
    Use neutral, professional sample content instead (e.g. landscapes, vehicles, everyday objects).
- Never add personal data to source files.
    This includes real usernames, home directories, machine-specific paths, secrets, API keys, tokens, passwords, private hostnames, real timestamps from local sessions, or identifiable project or dataset names.
    Use generic placeholders instead (e.g. `C:\Photos`, `C:\datasets\sample`, `sample_train_v1`, `2026-01-01T00:00:00.000Z`).
- Never manually modify CHANGELOG.md, TODO.md, or any files that are marked as auto-generated.
- When writing or substantially editing long Markdown files, put each full sentence on its own line.
    Preserve normal Markdown structure, but avoid wrapping multiple sentences onto one physical line.
- When making technical decisions, do not give much weight to development cost.
    Instead, prefer quality, simplicity, robustness, scalability, and long term maintainability.
- When doing bug fixes, always start with reproducing the bug in an E2E setting as closely aligned with how an end user would experience it as possible.
    This makes sure you find the real problem, so your fix will automatically solve it.
- When end-to-end testing a product, be picky about the UI you see and be obsessed with pixel perfection.
    If something clearly looks off, even if it is not directly related to what you are doing, try to get it fixed along.
- Apply that same high standard to engineering excellence: lint, test failures, and test flakiness.
    If you see one, even if it is not caused by what you are working on right now, still get it fixed.
- When fixing a bug, rather write a test case instead of leaving a comment in the code. Never leave unnecessary comments in the code and remove unnecessary comments if you come across them.
    The code should always be written in a way that is self-explanatory.
- Apply the 'good camper' principle of programming and improve code you come across, if it is a quick win with low effort.
