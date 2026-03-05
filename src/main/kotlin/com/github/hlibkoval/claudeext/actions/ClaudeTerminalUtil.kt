package com.github.hlibkoval.claudeext.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object ClaudeTerminalUtil {

    fun openSession(project: Project, extraArgs: List<String> = emptyList(), tabName: String = "Claude Code") {
        val basePath = project.basePath ?: return
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val claudeCmd = PluginSettings.getInstance().claudeCommand
        val shellCommand = mutableListOf(claudeCmd).apply {
            addAll(extraArgs)
        }
        val tabState = TerminalTabState().apply {
            myTabName = tabName
            myWorkingDirectory = basePath
            myShellCommand = shellCommand
        }
        terminalManager.createNewSession(terminalManager.terminalRunner, tabState)
        terminalManager.toolWindow?.activate(null)
    }
}
