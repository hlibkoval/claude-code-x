package com.github.hlibkoval.claudecodex.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.File

object ClaudeTerminalUtil {

    fun openSession(project: Project, extraArgs: List<String> = emptyList(), tabName: String = "Claude Code") {
        val basePath = project.basePath ?: return
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val claudeCmd = resolveCommand(PluginSettings.getInstance().claudeCommand)
        val shell = System.getenv("SHELL") ?: "/bin/zsh"
        val innerCmd = (listOf(claudeCmd) + extraArgs).joinToString(" ") { shellEscape(it) }
        val shellCommand = listOf(shell, "-lic", "exec $innerCmd")
        val tabState = TerminalTabState().apply {
            myTabName = tabName
            myWorkingDirectory = basePath
            myShellCommand = shellCommand
        }
        terminalManager.createNewSession(terminalManager.terminalRunner, tabState)
        terminalManager.toolWindow?.activate(null)
    }

    private val EXTRA_PATH_DIRS = listOf(
        "/opt/homebrew/bin", "/usr/local/bin",
        System.getProperty("user.home") + "/.local/bin",
        System.getProperty("user.home") + "/.npm/bin",
    )

    private fun resolveCommand(cmd: String): String {
        if (File(cmd).isAbsolute) return cmd
        val dirs = (System.getenv("PATH")?.split(File.pathSeparator).orEmpty() + EXTRA_PATH_DIRS).distinct()
        for (dir in dirs) {
            val file = File(dir, cmd)
            if (file.canExecute()) return file.absolutePath
        }
        return cmd
    }

    private fun shellEscape(s: String): String {
        if (s.all { it.isLetterOrDigit() || it in "/_.-+" }) return s
        return "'" + s.replace("'", "'\\''") + "'"
    }
}