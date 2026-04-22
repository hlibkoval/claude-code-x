package com.github.hlibkoval.claudecodex.actions

data class SessionTitleFixture(val label: String, val body: String)

object FixtureLoader {
    private const val SEPARATOR = "---FIXTURE---"

    fun loadSessionTitleFixtures(): List<SessionTitleFixture> {
        val resource = javaClass.classLoader.getResource("session-title-samples.txt")
            ?: error("session-title-samples.txt not found on the test classpath")
        return resource.readText().split(SEPARATOR).mapNotNull(::parseChunk)
    }

    private fun parseChunk(chunk: String): SessionTitleFixture? {
        val lines = chunk.lines()
        var label: String? = null
        var bodyStartIdx = -1
        for ((i, line) in lines.withIndex()) {
            val trimmed = line.trimStart()
            if (label == null) {
                if (trimmed.startsWith("# label:")) {
                    label = trimmed.substringAfter("# label:").trim()
                }
                continue
            }
            if (trimmed.startsWith("#") || trimmed.isBlank()) continue
            bodyStartIdx = i
            break
        }
        val resolvedLabel = label ?: return null
        val body = if (bodyStartIdx < 0) ""
        else lines.subList(bodyStartIdx, lines.size).joinToString("\n").trimEnd()
        return SessionTitleFixture(resolvedLabel, body)
    }
}
