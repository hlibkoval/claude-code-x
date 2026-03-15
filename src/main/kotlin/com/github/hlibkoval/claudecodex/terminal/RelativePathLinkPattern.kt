package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class RelativePathLinkPattern(
    private val projectRoot: String,
    resolveFile: (String) -> Boolean = { path -> File(path).isFile },
    createHyperlink: (String, Int?, Int?) -> HyperlinkInfo = { path, sl, el ->
        FileHyperlinkInfo(path, LocalFileSystem.getInstance(), sl, el)
    },
) : WrappingLinkPattern(resolveFile, createHyperlink) {

    // Two alternatives:
    // 1. Paths with slashes: src/main/File.kt, ./gradlew, ../CLAUDE.md
    // 2. Bare filenames with extension: build.gradle.kts, CLAUDE.md
    // Both preceded by whitespace, start of line, or open paren; stop at whitespace/colon/parens/@
    override val pathRegex = Regex(
        """(?:^|(?<=[\s(]))""" +
        """([a-zA-Z0-9._][^\s:()@]*(?:/[^\s:()@]+)+""" +  // alt 1: path with /
        """|[^\s:()@/]+\.[a-zA-Z0-9]+)""" +                 // alt 2: bare filename.ext
        """(?::(\d+)(?:-(\d+))?)?"""
    )

    override fun resolveToAbsolute(matchedPath: String): String = "$projectRoot/$matchedPath"
}
