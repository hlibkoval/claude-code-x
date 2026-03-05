package com.github.hlibkoval.claudeext.actions

import com.github.hlibkoval.claudeext.services.ClaudeSessionService
import com.intellij.openapi.actionSystem.*

class ResumeSessionActionGroup : DefaultActionGroup() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        val service = project.getService(ClaudeSessionService::class.java)
        e.presentation.isEnabled = service.hasSessions()
        e.presentation.isPerformGroup = true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val sessions = project.getService(ClaudeSessionService::class.java).listSessions()
        if (sessions.isNotEmpty()) {
            ClaudeTerminalUtil.openSession(
                project,
                listOf("--resume", sessions.first().id),
                "Claude Code (Resume)"
            )
        }
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val project = e?.project ?: return AnAction.EMPTY_ARRAY
        val service = project.getService(ClaudeSessionService::class.java)
        val sessions = service.listSessions()
        if (sessions.isEmpty()) return AnAction.EMPTY_ARRAY

        val actions = mutableListOf<AnAction>()

        for (session in sessions) {
            val name = session.slug
                ?: session.firstPrompt?.let { if (it.length > 40) it.take(40) + "..." else it }
                ?: session.id.take(8)
            val time = formatRelativeTime(session.modified)
            val sessionId = session.id
            actions.add(object : AnAction("$name ($time)") {
                override fun actionPerformed(e: AnActionEvent) {
                    val p = e.project ?: return
                    ClaudeTerminalUtil.openSession(p, listOf("--resume", sessionId), "Claude Code (Resume)")
                }
            })
        }

        actions.add(Separator.create())
        actions.add(object : AnAction("Browse All Sessions...") {
            override fun actionPerformed(e: AnActionEvent) {
                val p = e.project ?: return
                ClaudeTerminalUtil.openSession(p, listOf("--resume"), "Claude Code (Resume)")
            }
        })

        return actions.toTypedArray()
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
