package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class AbsolutePathLinkPattern(
    resolveFile: (String) -> Boolean = { path -> File(path).isFile },
    createHyperlink: (String, Int?, Int?) -> HyperlinkInfo = { path, sl, el ->
        FileHyperlinkInfo(path, LocalFileSystem.getInstance(), sl, el)
    },
    private val userHome: String = System.getProperty("user.home"),
) : WrappingLinkPattern(resolveFile, createHyperlink) {

    override val pathRegex = Regex("""(~?/[^\s:()@]+)(?::(\d+)(?:-(\d+))?)?""")

    override fun resolveToAbsolute(matchedPath: String): String =
        if (matchedPath.startsWith("~/")) userHome + matchedPath.substring(1)
        else matchedPath
}
