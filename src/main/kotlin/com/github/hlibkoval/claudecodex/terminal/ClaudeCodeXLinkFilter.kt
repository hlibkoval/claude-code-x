package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project

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
        val results = patterns.flatMap { it.processLine(line, lineStart) }
        return if (results.isEmpty()) null else Filter.Result(results)
    }
}
