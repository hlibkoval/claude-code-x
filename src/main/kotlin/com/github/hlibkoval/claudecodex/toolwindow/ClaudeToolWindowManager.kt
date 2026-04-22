package com.github.hlibkoval.claudecodex.toolwindow

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab

@Service(Service.Level.PROJECT)
class ClaudeToolWindowManager(private val project: Project) {

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

    companion object {
        const val TOOL_WINDOW_ID: String = "Claude Code"

        @JvmStatic
        fun getInstance(project: Project): ClaudeToolWindowManager = project.service()
    }
}
