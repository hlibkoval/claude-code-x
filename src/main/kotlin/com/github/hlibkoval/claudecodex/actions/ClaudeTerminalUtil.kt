package com.github.hlibkoval.claudecodex.actions

import com.anthropic.code.plugin.settings.PluginSettings
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.github.hlibkoval.claudecodex.startup.ClaudeEditorTabKeys
import com.github.hlibkoval.claudecodex.toolwindow.ClaudeToolWindowManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager

object ClaudeTerminalUtil {

    fun openSession(
        project: Project,
        extraArgs: List<String> = emptyList(),
        tabName: String = "Claude Code",
        forceInToolWindow: Boolean = false,
        requestFocus: Boolean = true,
    ) {
        launchClaude(
            project = project,
            extraArgs = extraArgs,
            tabName = tabName,
            forceInToolWindow = forceInToolWindow,
            requestFocus = requestFocus,
            marker = ClaudeEditorTabKeys.CLAUDE_TAB_MARKER,
        )
    }

    fun openAgentsView(
        project: Project,
        forceInToolWindow: Boolean = false,
        requestFocus: Boolean = true,
    ) {
        if (focusExistingAgentsInstance(project, requestFocus)) return
        launchClaude(
            project = project,
            extraArgs = listOf("agents"),
            tabName = "Claude Agents",
            forceInToolWindow = forceInToolWindow,
            requestFocus = requestFocus,
            marker = ClaudeEditorTabKeys.CLAUDE_AGENTS_TAB_MARKER,
        )
    }

    private fun focusExistingAgentsInstance(project: Project, requestFocus: Boolean): Boolean {
        val marker = ClaudeEditorTabKeys.CLAUDE_AGENTS_TAB_MARKER

        val editorFile = FileEditorManager.getInstance(project).openFiles
            .firstOrNull { it.getUserData(marker) == true }
        if (editorFile != null) {
            FileEditorManagerEx.getInstanceEx(project).openFile(
                file = editorFile,
                window = null,
                options = FileEditorOpenOptions(
                    selectAsCurrent = requestFocus,
                    reuseOpen = true,
                    requestFocus = requestFocus,
                    pin = true,
                ),
            )
            return true
        }

        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(ClaudeToolWindowManager.TOOL_WINDOW_ID)
        val content = toolWindow?.contentManager?.contents?.firstOrNull {
            it.getUserData(marker) == true
        }
        if (toolWindow != null && content != null) {
            val select = { toolWindow.contentManager.setSelectedContent(content, requestFocus) }
            if (requestFocus && !toolWindow.isActive) {
                toolWindow.activate(select, false, false)
            } else {
                select()
                if (!toolWindow.isVisible) toolWindow.show(null)
            }
            return true
        }
        return false
    }

    private fun launchClaude(
        project: Project,
        extraArgs: List<String>,
        tabName: String,
        forceInToolWindow: Boolean,
        requestFocus: Boolean,
        marker: Key<Boolean>,
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
            file.putUserData(marker, true)

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
            tab.content.putUserData(marker, true)
            ClaudeToolWindowManager.getInstance(project).attachTabToClaudeToolWindow(tab, requestFocus = requestFocus)
        }
    }
}
