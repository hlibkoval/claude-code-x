package com.github.hlibkoval.claudecodex.actions

import com.github.hlibkoval.claudecodex.services.ClaudeSessionService
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.impl.SplitButtonAction
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import java.awt.FontMetrics
import javax.swing.JLabel

class ClaudeCodeSplitAction : SplitButtonAction() {

    companion object {
        private const val MAX_TITLE_WIDTH_PX = 300
    }

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
            .asSequence()
            .mapNotNull { session ->
                val title = session.title?.let(SessionTitleParser::parse)?.takeIf { it.isNotBlank() }
                val prompt = session.firstPrompt?.let(SessionTitleParser::parse)?.takeIf { it.isNotBlank() }
                val raw = title ?: prompt ?: return@mapNotNull null
                Triple(session.id, raw, session.modified)
            }
            .take(50)
            .toList()

        val actions = mutableListOf<AnAction>()

        val settings = ClaudeCodeXSettings.getInstance(project)
        actions.add(object : ToggleAction("Open in Editor") {
            override fun isSelected(e: AnActionEvent): Boolean = settings.openInEditor
            override fun setSelected(e: AnActionEvent, state: Boolean) { settings.openInEditor = state }
        })
        actions.add(Separator.create())

        val fm = labelFontMetrics()
        val maxTitlePx = JBUIScale.scale(MAX_TITLE_WIDTH_PX)
        for ((sessionId, raw, modified) in sessions) {
            val name = truncateToWidth(raw, maxTitlePx, fm)
            val time = formatRelativeTime(modified)
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

    private fun labelFontMetrics(): FontMetrics {
        val label = JLabel()
        return label.getFontMetrics(UIUtil.getLabelFont())
    }

    private fun truncateToWidth(text: String, maxWidthPx: Int, fm: FontMetrics): String {
        if (fm.stringWidth(text) <= maxWidthPx) return text
        val ellipsis = "..."
        val budget = maxWidthPx - fm.stringWidth(ellipsis)
        if (budget <= 0) return ellipsis
        var lo = 0
        var hi = text.length
        while (lo < hi) {
            val mid = (lo + hi + 1) / 2
            if (fm.stringWidth(text.substring(0, mid)) <= budget) lo = mid else hi = mid - 1
        }
        return text.substring(0, lo).trimEnd() + ellipsis
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
