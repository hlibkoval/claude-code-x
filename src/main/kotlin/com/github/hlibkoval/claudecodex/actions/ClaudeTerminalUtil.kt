package com.github.hlibkoval.claudecodex.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.github.hlibkoval.claudecodex.startup.ClaudeEditorTabKeys
import com.github.hlibkoval.claudecodex.toolwindow.ClaudeToolWindowManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager

object ClaudeTerminalUtil {

    fun openSession(
        project: Project,
        extraArgs: List<String> = emptyList(),
        tabName: String = "Claude Code",
        forceInToolWindow: Boolean = false,
        requestFocus: Boolean = true,
    ) {
        val basePath = project.basePath ?: return
        val claudeCmd = (listOf(PluginSettings.getInstance().claudeCommand) + extraArgs).joinToString(" ")
        val shell = System.getenv("SHELL") ?: "/bin/zsh"
        val shellCommand = listOf(shell, "-lic", "exec $claudeCmd")

        val tabsManager = TerminalToolWindowTabsManager.getInstance(project)
        val openInEditor = !forceInToolWindow && ClaudeCodeXSettings.getInstance(project).openInEditor

        if (openInEditor) {
            val tab = tabsManager.createTabBuilder()
                .shellCommand(shellCommand)
                .workingDirectory(basePath)
                .tabName(tabName)
                .requestFocus(false)
                .createTab()
            val closeOnProcessTermination = tab.closeOnProcessTermination
            val view = tabsManager.detachTab(tab)
            val file = TerminalViewVirtualFileFactory.create(view, closeOnProcessTermination)
            file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
            file.putUserData(FileEditorManagerKeys.FORBID_TAB_SPLIT, true)
            file.putUserData(FileEditorManagerKeys.FORBID_PREVIEW_TAB, true)
            file.putUserData(ClaudeEditorTabKeys.CLAUDE_TAB_MARKER, true)

            val manager = FileEditorManagerEx.getInstanceEx(project)
            manager.openFile(
                file = file,
                window = null,
                options = FileEditorOpenOptions(
                    selectAsCurrent = requestFocus,
                    reuseOpen = true,
                    requestFocus = requestFocus,
                    pin = true,
                ),
            )
        } else {
            val tab = tabsManager.createTabBuilder()
                .shellCommand(shellCommand)
                .workingDirectory(basePath)
                .tabName(tabName)
                .requestFocus(requestFocus)
                .shouldAddToToolWindow(false)
                .createTab()
            ClaudeToolWindowManager.getInstance(project).attachTabToClaudeToolWindow(tab, requestFocus = requestFocus)
        }
    }
}
