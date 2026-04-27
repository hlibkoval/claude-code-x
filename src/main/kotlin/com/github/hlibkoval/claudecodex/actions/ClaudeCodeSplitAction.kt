package com.github.hlibkoval.claudecodex.actions

import com.github.hlibkoval.claudecodex.services.ClaudeSessionService
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupChooserBuilder
import com.intellij.openapi.wm.impl.SplitButtonAction
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Insets
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.border.AbstractBorder

class ClaudeCodeSplitAction : SplitButtonAction() {

    companion object {
        private const val MAX_TITLE_WIDTH_PX = 300
    }

    private sealed class SessionPopupItem {
        data class Session(
            val id: String,
            val displayTitle: String,
            val time: String,
            val searchText: String,
        ) : SessionPopupItem()

        data object BrowseAll : SessionPopupItem()
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
        val settings = ClaudeCodeXSettings.getInstance(project)

        val items = buildList<SessionPopupItem> {
            val fm = labelFontMetrics()
            val maxTitlePx = JBUIScale.scale(MAX_TITLE_WIDTH_PX)
            service.listSessions().asSequence()
                .mapNotNull { s ->
                    val title = s.title?.let(SessionTitleParser::parse)?.takeIf { it.isNotBlank() }
                    val prompt = s.firstPrompt?.let(SessionTitleParser::parse)?.takeIf { it.isNotBlank() }
                    val raw = title ?: prompt ?: return@mapNotNull null
                    SessionPopupItem.Session(
                        id = s.id,
                        displayTitle = truncateToWidth(raw, maxTitlePx, fm),
                        time = formatRelativeTime(s.modified),
                        searchText = raw,
                    )
                }
                .take(50)
                .forEach { add(it) }
            add(SessionPopupItem.BrowseAll)
        }

        val openInEditor = JBCheckBox("Open in editor", settings.openInEditor).apply {
            isOpaque = false
            addItemListener { settings.openInEditor = isSelected }
        }
        val southPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(4, 8)
            add(openInEditor, BorderLayout.WEST)
        }

        @Suppress("UNCHECKED_CAST")
        val builder = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(items) as PopupChooserBuilder<SessionPopupItem>
        builder.setSouthComponent(southPanel)

        val popup = builder
            .setRenderer(SessionPopupItemRenderer())
            .setNamerForFiltering { item ->
                when (item) {
                    is SessionPopupItem.Session -> item.searchText
                    SessionPopupItem.BrowseAll -> " "
                }
            }
            .setFilterAlwaysVisible(true)
            .setAutoPackHeightOnFiltering(true)
            .setItemChosenCallback { item ->
                when (item) {
                    is SessionPopupItem.Session ->
                        ClaudeTerminalUtil.openSession(
                            project, listOf("--resume", item.id), "Claude Code (Resume)"
                        )
                    SessionPopupItem.BrowseAll ->
                        ClaudeTerminalUtil.openSession(
                            project, listOf("--resume"), "Claude Code (Resume)"
                        )
                }
            }
            .setRequestFocus(true)
            .setResizable(true)
            .setMovable(false)
            .setVisibleRowCount(15)
            .createPopup()

        // `ListWithFilter` lays out its `SearchTextField` flush with the popup's
        // top edge and gives it a bottom-side line border plus intrinsic
        // text-field padding, leaving the field visually top-heavy. Replace the
        // border with explicit insets so top and bottom balance — the bottom
        // is negative to pull layout up past the JTextField's intrinsic
        // baseline padding.
        findSearchField(popup.content)?.border =
            FixedInsetsBorder(JBUIScale.scale(5), 0, -JBUIScale.scale(3), 0)
        return popup
    }

    private fun findSearchField(component: Component): SearchTextField? {
        if (component is SearchTextField) return component
        if (component is Container) {
            for (child in component.components) {
                findSearchField(child)?.let { return it }
            }
        }
        return null
    }

    private class FixedInsetsBorder(
        private val top: Int,
        private val left: Int,
        private val bottom: Int,
        private val right: Int,
    ) : AbstractBorder() {
        override fun getBorderInsets(c: Component): Insets = Insets(top, left, bottom, right)
        override fun getBorderInsets(c: Component, insets: Insets): Insets {
            insets.set(top, left, bottom, right)
            return insets
        }
        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {}
    }

    private class SessionPopupItemRenderer : ListCellRenderer<SessionPopupItem> {
        override fun getListCellRendererComponent(
            list: JList<out SessionPopupItem>,
            value: SessionPopupItem,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean,
        ): Component {
            val bg = UIUtil.getListBackground(isSelected, cellHasFocus)
            val fg = UIUtil.getListForeground(isSelected, cellHasFocus)
            val panel = JPanel(BorderLayout()).apply {
                background = bg
                border = JBUI.Borders.empty(2, 8)
            }
            when (value) {
                is SessionPopupItem.Session -> {
                    val title = JBLabel(value.displayTitle).apply {
                        foreground = fg
                        isOpaque = false
                    }
                    val time = JBLabel("  ${value.time}").apply {
                        foreground = if (isSelected) fg else UIUtil.getContextHelpForeground()
                        isOpaque = false
                    }
                    panel.add(title, BorderLayout.CENTER)
                    panel.add(time, BorderLayout.EAST)
                }
                SessionPopupItem.BrowseAll -> {
                    val label = JBLabel("Browse All Sessions…").apply {
                        foreground = fg
                        isOpaque = false
                        font = font.deriveFont(Font.ITALIC)
                    }
                    panel.add(label, BorderLayout.CENTER)
                }
            }
            return panel
        }
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
