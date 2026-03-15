package com.github.hlibkoval.claudecodex.terminal

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RelativePathLinkPatternTest {

    private val projectRoot = "/Users/gleb/Projects/jetbrains-claude-code-plugin"

    private val knownFiles = mutableSetOf(
        "$projectRoot/src/main/resources/META-INF/plugin.xml",
        "$projectRoot/src/main/kotlin/com/github/hlibkoval/claudecodex/actions/ClaudeTerminalUtil.kt",
        "$projectRoot/build.gradle.kts",
        "$projectRoot/observability/notebooks/057-payments-order-check-api-keys.ipynb",
        "$projectRoot/./gradlew",
    )

    private lateinit var pattern: RelativePathLinkPattern

    @BeforeEach
    fun setUp() {
        pattern = RelativePathLinkPattern(
            projectRoot = projectRoot,
            resolveFile = { it in knownFiles },
            createHyperlink = { path, sl, el -> TestHyperlinkInfo(path, sl, el) },
        )
    }

    // --- single-line matches ---

    @Test
    fun `matches relative path with slashes`() {
        val line = "see src/main/resources/META-INF/plugin.xml here"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/src/main/resources/META-INF/plugin.xml", info.path)
        assertEquals(4, links[0].highlightStartOffset)
        assertEquals(42, links[0].highlightEndOffset)
    }

    @Test
    fun `matches path preceded by open paren`() {
        val line = "Edit Notebook(observability/notebooks/057-payments-order-check-api-keys.ipynb)"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/observability/notebooks/057-payments-order-check-api-keys.ipynb", info.path)
    }

    @Test
    fun `stops at @ character`() {
        val line = "Edit Notebook(observability/notebooks/057-payments-order-check-api-keys.ipynb@dfqwzmj9spg)"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/observability/notebooks/057-payments-order-check-api-keys.ipynb", info.path)
    }

    @Test
    fun `matches bare filename with extension`() {
        val line = "see build.gradle.kts here"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/build.gradle.kts", info.path)
        assertEquals(4, links[0].highlightStartOffset)
        assertEquals(20, links[0].highlightEndOffset)
    }

    @Test
    fun `matches bare filename with single extension`() {
        knownFiles.add("$projectRoot/CLAUDE.md")
        val line = "- CLAUDE.md"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/CLAUDE.md", info.path)
    }

    @Test
    fun `bare filename ignores non-existent file`() {
        val line = "see nonexistent.xyz here"
        val results = pattern.processLine(line, 0)

        assertEquals(0, results.size)
    }

    @Test
    fun `bare filename with line number`() {
        val line = "- build.gradle.kts:10"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/build.gradle.kts", info.path)
        assertEquals(10, info.startLine)
    }

    @Test
    fun `matches path with line number`() {
        val line = "src/main/kotlin/com/github/hlibkoval/claudecodex/actions/ClaudeTerminalUtil.kt:42"
        val links = pattern.processLine(line, 0).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals(
            "$projectRoot/src/main/kotlin/com/github/hlibkoval/claudecodex/actions/ClaudeTerminalUtil.kt",
            info.path,
        )
        assertEquals(42, info.startLine)
    }

    @Test
    fun `does not match paths starting with slash`() {
        knownFiles.add("/etc/hosts")
        val line = "see /etc/hosts here"
        val results = pattern.processLine(line, 0)

        assertEquals(0, results.size)
    }

    // --- wrapped path tests ---

    @Test
    fun `handles wrapped relative path`() {
        val line1 = "src/main/kotlin/com/github/hlibkoval/claudecodex/actions/ClaudeTerminal"
        assertEquals(0, pattern.processLine(line1, 0).size)

        val line2 = "Util.kt"
        val links = pattern.processLine(line2, line1.length).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals(
            "$projectRoot/src/main/kotlin/com/github/hlibkoval/claudecodex/actions/ClaudeTerminalUtil.kt",
            info.path,
        )
        assertEquals(0, links[0].highlightStartOffset)
        assertEquals(line1.length + "Util.kt".length, links[0].highlightEndOffset)
    }

    @Test
    fun `handles wrapped path with indented continuation`() {
        val line1 = "Edit Notebook(observability/notebooks/057-payments-order-check-api-keys.ipyn"
        assertEquals(0, pattern.processLine(line1, 0).size)

        val line2 = "               b@dfqwzmj9spg)"
        val links = pattern.processLine(line2, line1.length).hyperlinks()

        assertEquals(1, links.size)
        val info = links[0].hyperlinkInfo as TestHyperlinkInfo
        assertEquals("$projectRoot/observability/notebooks/057-payments-order-check-api-keys.ipynb", info.path)
    }

    @Test
    fun `calculates offsets relative to lineStart`() {
        val line = " src/main/resources/META-INF/plugin.xml "
        val lineStart = 200
        val links = pattern.processLine(line, lineStart).hyperlinks()

        assertEquals(1, links.size)
        assertEquals(201, links[0].highlightStartOffset)
        assertEquals(239, links[0].highlightEndOffset)
    }
}
