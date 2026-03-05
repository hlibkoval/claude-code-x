package com.github.hlibkoval.claudeext.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.terminal.TerminalTabState
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

object ClaudeTerminalUtil {

    fun openClaudeSession(project: Project, extraArgs: String = "", tabName: String = "Claude Code") {
        val basePath = project.basePath ?: return
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val widget = terminalManager.createShellWidget(basePath, tabName, true, true)
        val command = buildString {
            append(PluginSettings.getInstance().claudeCommand)
            if (extraArgs.isNotBlank()) {
                append(" ")
                append(extraArgs)
            }
        }
        widget.sendCommandToExecute(command)
        terminalManager.toolWindow?.activate(null)
    }

    fun openClaudeDirect(project: Project, extraArgs: String = "", tabName: String = "Claude Code") {
        val basePath = project.basePath ?: return
        val terminalManager = TerminalToolWindowManager.getInstance(project)
        val claudeCmd = PluginSettings.getInstance().claudeCommand
        val shellCommand = mutableListOf(claudeCmd).apply {
            if (extraArgs.isNotBlank()) addAll(extraArgs.split(" "))
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