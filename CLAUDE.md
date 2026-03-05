# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Claude Code X — a companion IntelliJ plugin that extends the closed-source Anthropic Claude Code plugin (`com.anthropic.code.plugin`) with QoL UI/UX improvements. It depends on the official plugin at runtime and accesses its classes (e.g., `PluginSettings`).

## Build Commands

```bash
./gradlew buildPlugin          # Build the plugin (output in build/distributions/)
./gradlew runIde               # Launch a sandbox IDE with the plugin installed
./gradlew verifyPluginConfiguration  # Validate plugin.xml and compatibility
```

Requires JDK 21 (pinned via `.sdkmanrc`: `java=21-tem`). If sdkman is unavailable, set `JAVA_HOME` manually to a JDK 21 installation.

## Architecture

**Companion plugin pattern**: This plugin cannot modify the official Claude Code plugin. Instead, it declares `<depends>com.anthropic.code.plugin</depends>` in `plugin.xml` and adds its own toolbar actions alongside the original button in `MainToolbarRight`.

Key integration points with the official plugin:
- `PluginSettings.getInstance().claudeCommand` — gets the user-configured `claude` binary path
- Actions are positioned relative to `com.anthropic.code.plugin.actions.OpenClaudeInTerminalAction`

**Terminal launch**: Uses `TerminalToolWindowManager` to open Claude sessions. Two mechanisms exist:
- `createShellWidget()` + `sendCommandToExecute()` — shell-based (legacy, being removed)
- `TerminalTabState.myShellCommand` + `createNewSession()` — direct launch (preferred: faster startup, cleaner env, tab auto-closes on exit)

**Session data**: Claude Code stores sessions as `~/.claude/projects/<project-hash>/<uuid>.jsonl` where `<project-hash>` is the absolute project path with `/` replaced by `-` (e.g., `-Users-gleb-Projects-foo`). Some projects also have `sessions-index.json` with pre-computed metadata.

## Key Conventions

- All actions extend `AnAction`, use `ActionUpdateThread.EDT`, and guard on `e.project != null`
- Plugin targets IntelliJ Platform 2024.2+ (`sinceBuild = "242"`)
- The official plugin JAR lives in `claude-code-jetbrains-plugin/lib/` (gitignored) and is declared as a Gradle dependency via `plugin("com.anthropic.code.plugin", "0.1.14-beta")`
