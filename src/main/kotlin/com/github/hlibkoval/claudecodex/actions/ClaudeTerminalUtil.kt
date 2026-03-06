package com.github.hlibkoval.claudecodex.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager

object ClaudeTerminalUtil {

    fun openSession(project: Project, extraArgs: List<String> = emptyList(), tabName: String = "Claude Code") {
        val basePath = project.basePath ?: return
        val allArgs = buildList {
            addAll(extraArgs)
            if (ClaudeCodeXSettings.getInstance(project).dangerouslySkipPermissions) {
                add("--dangerously-skip-permissions")
            }
        }
        val claudeCmd = (listOf(PluginSettings.getInstance().claudeCommand) + allArgs).joinToString(" ")
        val shell = System.getenv("SHELL") ?: "/bin/zsh"
        val shellCommand = listOf(shell, "-lic", "exec $claudeCmd")

        val tabsManager = TerminalToolWindowTabsManager.getInstance(project)
        val openInEditor = ClaudeCodeXSettings.getInstance(project).openInEditor

        val tab = tabsManager.createTabBuilder()
            .shellCommand(shellCommand)
            .workingDirectory(basePath)
            .tabName(tabName)
            .requestFocus(true)
            .createTab()

        if (openInEditor) {
            val view = tabsManager.detachTab(tab)
            val file = TerminalViewVirtualFileFactory.create(view)
            file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
            try {
                FileEditorManager.getInstance(project).openFile(file, true)
            } finally {
                file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, null)
            }
        } else {
            ToolWindowManager.getInstance(project).getToolWindow("Terminal")?.activate(null)
        }
    }
}
