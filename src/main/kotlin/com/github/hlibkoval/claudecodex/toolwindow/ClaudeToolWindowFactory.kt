package com.github.hlibkoval.claudecodex.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

class ClaudeToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        project.messageBus.connect(toolWindow.disposable)
            .subscribe(ToolWindowManagerListener.TOPIC, object : ToolWindowManagerListener {
                override fun toolWindowShown(shown: ToolWindow) {
                    ClaudeToolWindowManager.getInstance(project).autoStartIfEmpty(shown)
                }
            })
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
