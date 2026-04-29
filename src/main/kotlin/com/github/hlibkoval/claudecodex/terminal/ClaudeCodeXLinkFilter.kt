package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project
import com.intellij.util.io.URLUtil
import java.util.regex.Pattern

class ClaudeCodeXLinkFilter(private val project: Project) : Filter {

    private val fileCache = FileExistenceCache()

    private val patterns: List<LinkPattern> = buildList {
        add(AbsolutePathLinkPattern(resolveFile = fileCache::isFile))
        val projectPath = project.basePath
        if (projectPath != null) {
            add(RelativePathLinkPattern(projectPath, resolveFile = fileCache::isFile))
        }
    }

    override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        val lineStart = entireLength - line.length
        val excluded = computeExcludedRanges(line, lineStart)
        val results = patterns
            .flatMap { it.processLine(line, lineStart) }
            .filterNot { item -> excluded.any { it.contains(item.highlightStartOffset) } }
        return if (results.isEmpty()) null else Filter.Result(results)
    }

    companion object {
        // Java/Kotlin stack-trace tail: (FileName.ext:line) or (FileName.ext:line:col).
        // Anchored on parens to avoid swallowing bare "Foo.kt:42" mentions in prose.
        private val STACK_TRACE_FRAME: Pattern =
            Pattern.compile("""\(([\w.$]+\.\w+):\d+(?::\d+)?\)""")

        // Patterns the platform's UrlFilter already covers — keep them in sync by reusing
        // the same constants UrlFilter uses.
        private val EXCLUDED_PATTERNS: List<Pattern> = listOf(
            URLUtil.URL_PATTERN_OPTIMIZED,
            URLUtil.FILE_URL_PATTERN_OPTIMIZED,
            STACK_TRACE_FRAME,
        )

        internal fun computeExcludedRanges(line: String, lineStart: Int): List<IntRange> {
            val ranges = mutableListOf<IntRange>()
            for (pattern in EXCLUDED_PATTERNS) {
                val matcher = pattern.matcher(line)
                while (matcher.find()) {
                    ranges.add(lineStart + matcher.start() until lineStart + matcher.end())
                }
            }
            return ranges
        }
    }
}
