package com.github.hlibkoval.claudecodex.toolwindow

import com.github.hlibkoval.claudecodex.actions.ClaudeTerminalUtil
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.github.hlibkoval.claudecodex.startup.ClaudeEditorTabKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class ClaudeToolWindowManager(private val project: Project) {

    private val autoStartInFlight = AtomicBoolean(false)

    fun attachTabToClaudeToolWindow(tab: TerminalToolWindowTab, requestFocus: Boolean) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return
        val contentManager = toolWindow.contentManager
        contentManager.addContent(tab.content)

        val select = { contentManager.setSelectedContent(tab.content, requestFocus) }
        if (requestFocus && !toolWindow.isActive) {
            toolWindow.activate(select, false, false)
        } else {
            select()
            if (!toolWindow.isVisible) toolWindow.show(null)
        }
    }

    fun autoStartIfEmpty(toolWindow: ToolWindow) {
        if (toolWindow.id != TOOL_WINDOW_ID) return
        if (toolWindow.contentManager.contents.isNotEmpty()) return
        if (ClaudeCodeXSettings.getInstance(project).openInEditor) return
        if (hasClaudeEditorTab()) return
        if (!autoStartInFlight.compareAndSet(false, true)) return

        ApplicationManager.getApplication().invokeLater({
            try {
                if (toolWindow.contentManager.contents.isEmpty()) {
                    ClaudeTerminalUtil.openSession(project, forceInToolWindow = true)
                }
            } finally {
                autoStartInFlight.set(false)
            }
        }, ModalityState.defaultModalityState(), project.disposed)
    }

    private fun hasClaudeEditorTab(): Boolean {
        return FileEditorManager.getInstance(project).openFiles.any {
            it.getUserData(ClaudeEditorTabKeys.CLAUDE_TAB_MARKER) == true
        }
    }

    companion object {
        const val TOOL_WINDOW_ID: String = "Claude Code"

        @JvmStatic
        fun getInstance(project: Project): ClaudeToolWindowManager = project.service()
    }
}
