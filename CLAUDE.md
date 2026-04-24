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

**Terminal launch**: Uses the reworked `TerminalToolWindowTabsManager` API (2025.3+) to open Claude sessions via `createTabBuilder().shellCommand().createTab()`. Supports opening in the editor area by detaching the tab and wrapping it in `TerminalViewVirtualFile` (requires `CLOSING_TO_REOPEN` user data key).

**Session data**: Claude Code stores sessions as `~/.claude/projects/<project-hash>/<uuid>.jsonl` where `<project-hash>` is the absolute project path with `/` replaced by `-` (e.g., `-Users-gleb-Projects-foo`). Some projects also have `sessions-index.json` with pre-computed metadata.

## Research

When researching IntelliJ Platform APIs, use context7 MCP before relying on training data. Relevant libraries (all High reputation):
- `/websites/plugins_jetbrains` — broadest coverage (9k+ snippets, score 82), use as primary source
- `/jetbrains/intellij-sdk-docs` — GitHub source with inline code examples, good for action system / plugin.xml / extension points
- `/websites/plugins_jetbrains_intellij` — narrower SDK-only subset, use when the broader source returns too much noise

When adding new features, start by querying the "IntelliJ Platform Extension Point and Listener List" from `/websites/plugins_jetbrains` to discover relevant extension points and listeners before writing code. This is the canonical reference for what the platform exposes.

**Local platform source** (gitignored): a shallow clone of `JetBrains/intellij-community` pinned to branch `261` (2026.1) lives at `intellij-community/` in the project root. Use it for grep/glob searches over real platform source — extension point definitions (`*ExtensionPoints.xml`), EP interfaces, `*Manager`/`*Service` classes, UI bundle keys, and reference implementations — without round-tripping to the web. Prefer this over GitHub search when you need to read code locally or chain searches. Keep context7 as the source of truth for narrative docs and API syntax; use the clone for source reading.

**API exploration techniques** (ref: https://plugins.jetbrains.com/docs/intellij/explore-api.html):
- **Extension points**: query the EP list first, then grep `intellij-community/` for `*ExtensionPoints.xml`; use IntelliJ Platform Explorer to find real implementations in open-source plugins
- **Source navigation**: grep `intellij-community/` for EP interfaces, `*Manager` / `*Service` classes, and related utilities in the interface's package
- **UI reverse-lookup**: grep `intellij-community/` for visible UI strings/bundle keys to find the underlying implementation
- **Debugging**: set breakpoints on `DocumentImpl.changedUpdate()` (doc changes), `HighlightInfoHolder.add()` (highlights), `OpenFileDescriptor` ctor (navigation), `GeneralCommandLine` ctor (process launch), `LightweightHint`/`ActionPopupMenuImpl` ctor (popups)
- **Avoid internal APIs**: skip classes ending in `Impl`, in `impl` packages, or annotated `@ApiStatus.Internal`

## Shell Command Conventions

When running `javap` or similar class inspection commands, call the binary directly with inline paths — do NOT assign variables first. This avoids permission prompt mismatches.

```bash
# GOOD — matches allow rule
javap -classpath /path/to/jar com.example.ClassName

# BAD — variable assignment prefix breaks allow rule matching
TERMINAL_JAR="/path/to/jar" && javap -classpath "$TERMINAL_JAR" com.example.ClassName
```

## Key Conventions

- All actions extend `AnAction`, use `ActionUpdateThread.EDT`, and guard on `e.project != null`
- Plugin targets IntelliJ Platform 2026.1+ (`sinceBuild = "261"`)
- The official plugin JAR lives in `claude-code-jetbrains-plugin/lib/` (gitignored) and is declared as a Gradle dependency via `plugin("com.anthropic.code.plugin", "0.1.14-beta")`
- CFR-decompiled sources of the official plugin are in `claude-code-jetbrains-plugin/decompiled/` — consult these when you need to understand official plugin internals (class signatures, method behavior, extension points) before writing integration code
