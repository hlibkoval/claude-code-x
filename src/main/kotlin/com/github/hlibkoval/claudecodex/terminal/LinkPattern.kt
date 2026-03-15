package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor

interface LinkPattern {
    fun processLine(line: String, lineStart: Int): List<Filter.ResultItem>
}

abstract class WrappingLinkPattern(
    private val resolveFile: (String) -> Boolean,
    private val createHyperlink: (path: String, startLine: Int?, endLine: Int?) -> HyperlinkInfo,
) : LinkPattern {

    protected abstract val pathRegex: Regex
    protected abstract fun resolveToAbsolute(matchedPath: String): String?

    private var pending: PendingPath? = null

    private data class PendingPath(
        val pathPrefix: String,
        val highlightStart: Int,
        val startLine: Int?,
        val endLine: Int?,
    )

    private val continuationRegex = Regex("""^\s*([^\s:()@]+)(?::(\d+)(?:-(\d+))?)?""")

    override fun processLine(line: String, lineStart: Int): List<Filter.ResultItem> {
        val results = mutableListOf<Filter.ResultItem>()

        // Step 1: try completing pending from previous line
        val pendingPath = pending
        pending = null

        if (pendingPath != null) {
            val completed = tryCompletePending(pendingPath, line, lineStart)
            if (completed != null) {
                results.add(completed)
            }
        }

        // Step 2: find new matches in this line
        val lineContent = line.trimEnd('\n', '\r')
        for (match in pathRegex.findAll(line)) {
            val pathGroup = match.groups[1] ?: continue
            val matchedPath = pathGroup.value
            val startLineNum = match.groups[2]?.value?.toIntOrNull()
            val endLineNum = match.groups[3]?.value?.toIntOrNull()

            val matchStartOffset = lineStart + pathGroup.range.first
            val matchEndOffset = lineStart + match.range.last + 1

            val absolutePath = resolveToAbsolute(matchedPath)

            if (absolutePath != null && resolveFile(absolutePath)) {
                // Decoration-only item (null HyperlinkInfo): provides underline styling
                // without conflicting with the official filter's hyperlink for the same range.
                // For click behavior, we rely on the official plugin's filter.
                results.add(decorationItem(matchStartOffset, matchEndOffset))
                // Also add a hyperlink item in case the official filter doesn't match this path
                val info = createHyperlink(absolutePath, startLineNum, endLineNum)
                results.add(styledResultItem(matchStartOffset, matchEndOffset, info))
            } else if (absolutePath != null && matchExtendsToEol(pathGroup, lineContent)) {
                // Path reaches EOL and doesn't resolve — suspect line wrap
                pending = PendingPath(
                    pathPrefix = matchedPath,
                    highlightStart = matchStartOffset,
                    startLine = startLineNum,
                    endLine = endLineNum,
                )
            }
        }

        return results
    }

    private fun matchExtendsToEol(pathGroup: MatchGroup, lineContent: String): Boolean {
        return pathGroup.range.last >= lineContent.length - 1
    }

    private fun tryCompletePending(
        pendingPath: PendingPath,
        line: String,
        lineStart: Int,
    ): Filter.ResultItem? {
        val continuationMatch = continuationRegex.find(line) ?: return null
        val suffix = continuationMatch.groups[1]?.value ?: return null
        val fullMatchedPath = pendingPath.pathPrefix + suffix
        val absolutePath = resolveToAbsolute(fullMatchedPath) ?: return null

        if (!resolveFile(absolutePath)) return null

        val startLineNum = pendingPath.startLine
            ?: continuationMatch.groups[2]?.value?.toIntOrNull()
        val endLineNum = pendingPath.endLine
            ?: continuationMatch.groups[3]?.value?.toIntOrNull()

        val highlightEnd = lineStart + continuationMatch.range.last + 1
        val info = createHyperlink(absolutePath, startLineNum, endLineNum)
        return styledResultItem(pendingPath.highlightStart, highlightEnd, info)
    }

    companion object {
        private val LINK_ATTRIBUTES = TextAttributes().apply {
            effectType = EffectType.LINE_UNDERSCORE
            effectColor = JBColor.namedColor("Terminal.Hyperlink.foreground", JBColor(0x589DF6, 0x589DF6))
            foregroundColor = JBColor.namedColor("Terminal.Hyperlink.foreground", JBColor(0x589DF6, 0x589DF6))
        }

        fun decorationItem(start: Int, end: Int): Filter.ResultItem {
            return object : Filter.ResultItem(start, end, null, LINK_ATTRIBUTES) {
                override fun getHighlighterLayer(): Int = HighlighterLayer.HYPERLINK + 1
            }
        }

        fun styledResultItem(start: Int, end: Int, info: HyperlinkInfo): Filter.ResultItem {
            return object : Filter.ResultItem(start, end, info, LINK_ATTRIBUTES) {
                override fun getHighlighterLayer(): Int = HighlighterLayer.HYPERLINK + 1
            }
        }
    }
}
