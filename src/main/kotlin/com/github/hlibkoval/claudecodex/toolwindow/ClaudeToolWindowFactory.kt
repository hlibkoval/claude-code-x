package com.github.hlibkoval.claudecodex.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class ClaudeToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Tabs are added on demand by ClaudeToolWindowManager; no initial content.
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
