package com.github.hlibkoval.claudecodex.actions

import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.github.hlibkoval.claudecodex.startup.ClaudeEditorTabKeys
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenClaudeInEditorAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = ClaudeCodeXSettings.getInstance(project)
        val previousOpenInEditor = settings.openInEditor
        settings.openInEditor = true
        try {
            if (settings.useAgentsMode) {
                ClaudeTerminalUtil.openAgentsView(project, requestFocus = true)
            } else {
                ClaudeTerminalUtil.openSession(project, requestFocus = true)
            }
        } finally {
            settings.openInEditor = previousOpenInEditor
        }
        val props = PropertiesComponent.getInstance(project)
        props.unsetValue(ClaudeEditorTabKeys.PROP_USER_CLOSED_THIS_SESSION)
        props.setValue(ClaudeEditorTabKeys.PROP_LAST_OPEN_INTENT, true)
    }
}
