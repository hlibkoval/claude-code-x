package com.github.hlibkoval.claudecodex.terminal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClaudeCodeXLinkFilterTest {

    private fun rangesFor(line: String, lineStart: Int = 0): List<IntRange> =
        ClaudeCodeXLinkFilter.computeExcludedRanges(line, lineStart)

    @Test
    fun `excludes http URL`() {
        val line = "see https://example.com/path here"
        val ranges = rangesFor(line)
        assertEquals(1, ranges.size)
        val covered = line.substring(ranges[0].first, ranges[0].last + 1)
        assertEquals("https://example.com/path", covered)
    }

    @Test
    fun `excludes file URL`() {
        val line = "open file:///tmp/foo.kt for details"
        val ranges = rangesFor(line)
        assertTrue(ranges.any { range ->
            line.substring(range.first, range.last + 1).startsWith("file:///tmp/foo.kt")
        })
    }

    @Test
    fun `excludes Java stack-trace frame`() {
        val line = "\tat com.example.Foo.bar(Foo.java:42)"
        val ranges = rangesFor(line)
        assertEquals(1, ranges.size)
        val covered = line.substring(ranges[0].first, ranges[0].last + 1)
        assertEquals("(Foo.java:42)", covered)
    }

    @Test
    fun `excludes stack-trace frame with column`() {
        val line = "\tat foo.Bar(Bar.kt:10:5)"
        val ranges = rangesFor(line)
        assertEquals(1, ranges.size)
        val covered = line.substring(ranges[0].first, ranges[0].last + 1)
        assertEquals("(Bar.kt:10:5)", covered)
    }

    @Test
    fun `does not exclude bare path mentions`() {
        val line = "edit src/main/Foo.kt:42 please"
        assertEquals(emptyList<IntRange>(), rangesFor(line))
    }

    @Test
    fun `does not exclude bare absolute path`() {
        val line = "see /Users/gleb/.zshrc here"
        assertEquals(emptyList<IntRange>(), rangesFor(line))
    }

    @Test
    fun `applies lineStart offset to ranges`() {
        val line = "https://example.com"
        val ranges = rangesFor(line, lineStart = 100)
        assertEquals(1, ranges.size)
        assertEquals(100, ranges[0].first)
        assertEquals(100 + line.length - 1, ranges[0].last)
    }

    @Test
    fun `excludes both URL and stack frame on same line`() {
        val line = "url https://x.test/y crash at p.Q(Q.java:1)"
        val ranges = rangesFor(line)
        assertEquals(2, ranges.size)
    }
}
