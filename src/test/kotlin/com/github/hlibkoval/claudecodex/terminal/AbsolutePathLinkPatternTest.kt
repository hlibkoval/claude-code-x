package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Records the arguments passed to createHyperlink for assertion. */
data class TestHyperlinkInfo(
    val path: String,
    val startLine: Int?,
    val endLine: Int?,
) : HyperlinkInfo {
    override fun navigate(project: Project) {}
}

/** Extract the hyperlink ResultItem (non-null HyperlinkInfo) from results. */
fun List<Filter.ResultItem>.hyperlinks(): List<Filter.ResultItem> =
    filter { it.hyperlinkInfo != null }

/** Extract decoration ResultItems (null HyperlinkInfo) from results. */
fun List<Filter.ResultItem>.decorations(): List<Filter.ResultItem> =
    filter { it.hyperlinkInfo == null }

class AbsolutePathLinkPatternTest {

    private val knownFiles = mutableSetOf(
        "/Users/gleb/file.txt",
        "/Users/gleb/Projects/jetbrains-claude-code-plugin/README.md",
        "/Users/gleb/Projects/foo/bar.kt",
        "/Users/gleb/Projects/foo/baz.txt",
        "/Users/gleb/Projects/subdir/file.txt",
        "/path/notebook.ipynb",
    )

    private lateinit var pattern: AbsolutePathLinkPattern

    @BeforeEach
    fun setUp() {
        pattern = AbsolutePathLinkPattern(
            resolveFile = { it in knownFiles },
            createHyperlink = { path, sl, el -> TestHyperlinkInfo(path, sl, el) },
        )
    }

    // --- single-line matches ---

    @Test
    fun `matches simple absolute path`() {
        val line = "see /Users/gleb/file.txt here"
        val results = pattern.processLine(line, 0)
        val links = results.hyperlinks()

        assertEquals(1, links.size)
        val r = links[0]
        assertEquals(4, r.highlightStartOffset)
        assertEquals(24, r.highlightEndOffset)
        val info = r.hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/file.txt", info.path)
        assertNull(info.startLine)
        // Should also have a decoration item
        assertEquals(1, results.decorations().size)
    }

    @Test
    fun `matches path with line number`() {
        val line = " /Users/gleb/Projects/foo/bar.kt:42 "
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/Projects/foo/bar.kt", info.path)
        assertEquals(42, info.startLine)
        assertNull(info.endLine)
    }

    @Test
    fun `matches path with line range`() {
        val line = "/Users/gleb/Projects/foo/bar.kt:10-20"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/Projects/foo/bar.kt", info.path)
        assertEquals(10, info.startLine)
        assertEquals(20, info.endLine)
    }

    @Test
    fun `ignores non-existent file`() {
        val line = "see /no/such/file.txt here"
        val results = pattern.processLine(line, 0)

        assertEquals(0, results.size)
    }

    @Test
    fun `matches multiple paths on same line`() {
        val line = "/Users/gleb/Projects/foo/bar.kt and /Users/gleb/Projects/foo/baz.txt"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(2, links.size)
        assertEquals("/Users/gleb/Projects/foo/bar.kt", (links[0].hyperlinkInfo as TestHyperlinkInfo).path)
        assertEquals("/Users/gleb/Projects/foo/baz.txt", (links[1].hyperlinkInfo as TestHyperlinkInfo).path)
    }

    @Test
    fun `stops at @ character`() {
        val line = "/path/notebook.ipynb@dfqwzmj9spg"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/path/notebook.ipynb", info.path)
        assertEquals(20, links[0].highlightEndOffset)
    }

    @Test
    fun `calculates offsets relative to entireLength`() {
        val line = "/Users/gleb/file.txt"
        val lineStart = 100
        val links = pattern.processLine(line, lineStart).hyperlinks()

        assertEquals(1, links.size)
        assertEquals(100, links[0].highlightStartOffset)
        assertEquals(120, links[0].highlightEndOffset)
    }

    // --- wrapped path tests ---

    @Test
    fun `handles wrapped path across two lines`() {
        val line1 = "/Users/gleb/Projects/jetbrains-claude-code-plug"
        val results1 = pattern.processLine(line1, 0)
        assertEquals(0, results1.size, "line 1 should not produce results (pending)")

        val line2 = "in/README.md"
        val line2Start = line1.length
        val links2 = pattern.processLine(line2, line2Start).hyperlinks()

        assertEquals(1, links2.size)
        val info = links2[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/Projects/jetbrains-claude-code-plugin/README.md", info.path)
        assertEquals(0, links2[0].highlightStartOffset)
        assertEquals(line1.length + "in/README.md".length, links2[0].highlightEndOffset)
    }

    @Test
    fun `handles wrapped path with indented continuation`() {
        val line1 = "/Users/gleb/Projects/jetbrains-claude-code-plug"
        val results1 = pattern.processLine(line1, 0)
        assertEquals(0, results1.size)

        val line2 = "               in/README.md"
        val line2Start = line1.length
        val links2 = pattern.processLine(line2, line2Start).hyperlinks()

        assertEquals(1, links2.size)
        val info = links2[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/Projects/jetbrains-claude-code-plugin/README.md", info.path)
        assertEquals(0, links2[0].highlightStartOffset)
        assertEquals(line2Start + line2.length, links2[0].highlightEndOffset)
    }

    @Test
    fun `handles wrap at slash boundary`() {
        val line1 = "/Users/gleb/Projects/"
        assertEquals(0, pattern.processLine(line1, 0).size)

        val line2 = "subdir/file.txt"
        val links = pattern.processLine(line2, line1.length).hyperlinks()

        assertEquals(1, links.size)
        assertEquals("/Users/gleb/Projects/subdir/file.txt", (links[0].hyperlinkInfo as TestHyperlinkInfo).path)
    }

    @Test
    fun `wrapped path with line number on continuation`() {
        val line1 = "/Users/gleb/Projects/foo/bar"
        assertEquals(0, pattern.processLine(line1, 0).size)

        val line2 = ".kt:42"
        val links = pattern.processLine(line2, line1.length).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("/Users/gleb/Projects/foo/bar.kt", info.path)
        assertEquals(42, info.startLine)
    }

    @Test
    fun `discards pending if continuation does not form valid file`() {
        pattern.processLine("/no/such/path/prefix", 0)
        val results2 = pattern.processLine("/something/else.txt", 20)
        assertEquals(0, results2.size)
    }

    @Test
    fun `pending is consumed even if continuation is empty line`() {
        val line1 = "/no/such/path/prefix"
        pattern.processLine(line1, 0)
        pattern.processLine("", line1.length)

        val results = pattern.processLine("/Users/gleb/file.txt", line1.length)
        assertEquals(1, results.hyperlinks().size)
    }
}
