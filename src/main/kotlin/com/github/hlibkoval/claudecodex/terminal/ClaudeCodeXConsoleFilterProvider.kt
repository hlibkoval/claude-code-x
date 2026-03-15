package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.openapi.project.Project

class ClaudeCodeXConsoleFilterProvider : ConsoleFilterProvider {
    override fun getDefaultFilters(project: Project): Array<Filter> {
        return arrayOf(ClaudeCodeXLinkFilter(project))
    }
}
