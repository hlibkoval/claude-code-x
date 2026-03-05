package com.github.hlibkoval.claudecodex.actions

import com.github.hlibkoval.claudecodex.services.ClaudeSessionService
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.impl.SplitButtonAction

class ClaudeCodeSplitAction : SplitButtonAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ClaudeTerminalUtil.openSession(project)
    }

    override fun createPopup(e: AnActionEvent): JBPopup {
        val project = e.project!!
        val service = project.getService(ClaudeSessionService::class.java)
        val sessions = service.listSessions()

        val actions = mutableListOf<AnAction>()

        for (session in sessions) {
            val name = session.title
                ?: session.firstPrompt?.let { if (it.length > 40) it.take(40) + "..." else it }
                ?: session.id.take(8)
            val time = formatRelativeTime(session.modified)
            val sessionId = session.id
            actions.add(object : AnAction("$name ($time)") {
                override fun actionPerformed(e: AnActionEvent) {
                    ClaudeTerminalUtil.openSession(project, listOf("--resume", sessionId), "Claude Code (Resume)")
                }
            })
        }

        actions.add(Separator.create())
        actions.add(object : AnAction("Browse All Sessions...") {
            override fun actionPerformed(e: AnActionEvent) {
                ClaudeTerminalUtil.openSession(project, listOf("--resume"), "Claude Code (Resume)")
            }
        })

        val group = DefaultActionGroup(actions)
        return JBPopupFactory.getInstance().createActionGroupPopup(
            null, group, e.dataContext,
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false
        )
    }

    private fun formatRelativeTime(epochMillis: Long): String {
        val diff = System.currentTimeMillis() - epochMillis
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 30 -> "${days}d ago"
            else -> "${days / 30}mo ago"
        }
    }
}
