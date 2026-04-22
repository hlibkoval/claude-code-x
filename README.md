# Claude Code X

Intellij Platform plugin with QoL extensions for the official [Claude Code](https://docs.anthropic.com/en/docs/claude-code) plugin by Anthropic.

> **Early development** — expect breaking changes.

## Features

- **New Session** button — launch a fresh Claude Code terminal session from the toolbar
- **Resume Session** — quickly resume previous Claude Code sessions with a split button and session picker dropdown
- **Direct terminal launch** — runs `claude` as the terminal process directly (faster startup, cleaner environment, auto-closes on exit)
- **Open in Editor** — per-project toggle to open Claude sessions as editor tabs instead of terminal tool window tabs

## Requirements

- IntelliJ-based IDE 2025.3+
- [Claude Code JetBrains plugin](https://plugins.jetbrains.com/plugin/27310-claude-code-beta-) (Beta) by Anthropic — install this first

## Installation

1. Download the latest `.zip` from [Releases](https://github.com/hlibkoval/claude-code-x/releases).
2. In your IDE, open **Settings → Plugins**, click the gear icon, and choose **Install Plugin from Disk…**.
3. Select the downloaded `.zip` and restart the IDE when prompted.

## License

[MIT](LICENSE)
