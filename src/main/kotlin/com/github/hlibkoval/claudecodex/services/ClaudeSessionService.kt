package com.github.hlibkoval.claudecodex.services

import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import java.io.File
import java.io.RandomAccessFile

class ClaudeSessionService(private val project: Project) {

    data class ClaudeSession(
        val id: String,
        val title: String?,
        val modified: Long,
        val firstPrompt: String?
    )

    fun getProjectSessionDir(): File? {
        val basePath = project.basePath ?: return null
        val hash = basePath.replace("/", "-")
        val dir = File(System.getProperty("user.home"), ".claude/projects/$hash")
        return if (dir.isDirectory) dir else null
    }

    fun hasSessions(): Boolean {
        val dir = getProjectSessionDir() ?: return false
        return dir.listFiles { f -> f.extension == "jsonl" }?.isNotEmpty() == true
    }

    fun listSessions(): List<ClaudeSession> {
        val dir = getProjectSessionDir() ?: return emptyList()

        val indexFile = File(dir, "sessions-index.json")
        if (indexFile.exists()) {
            try {
                val sessions = parseSessionsIndex(indexFile, dir)
                if (sessions.isNotEmpty()) return sessions
            } catch (_: Exception) {
            }
        }

        return dir.listFiles { f -> f.extension == "jsonl" }
            ?.map { parseJsonlSession(it) }
            ?.sortedByDescending { it.modified }
            ?: emptyList()
    }

    private fun parseSessionsIndex(indexFile: File, dir: File): List<ClaudeSession> {
        val json = JsonParser.parseString(indexFile.readText())
        val root = if (json.isJsonObject) json.asJsonObject else return emptyList()
        val entries = root.getAsJsonArray("entries") ?: return emptyList()
        return entries.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val obj = element.asJsonObject
            val sessionId = obj.get("sessionId")?.asString ?: return@mapNotNull null
            val summary = obj.get("summary")?.asString
            val firstPrompt = obj.get("firstPrompt")?.asString
            val modified = obj.get("modified")?.asString?.let { parseIsoTimestamp(it) }
                ?: obj.get("fileMtime")?.asLong ?: 0L
            // Check for customTitle in the JSONL file (overrides summary)
            val jsonlFile = File(dir, "$sessionId.jsonl")
            val customTitle = if (jsonlFile.exists()) readCustomTitle(jsonlFile) else null
            ClaudeSession(sessionId, customTitle ?: summary, modified, firstPrompt)
        }.sortedByDescending { it.modified }
    }

    private fun parseJsonlSession(file: File): ClaudeSession {
        val id = file.nameWithoutExtension
        var customTitle: String? = null
        var firstPrompt: String? = null

        // Read first 20 lines for firstPrompt
        try {
            file.bufferedReader().use { reader ->
                var linesRead = 0
                for (line in reader.lineSequence()) {
                    if (linesRead++ >= 20) break
                    try {
                        val obj = JsonParser.parseString(line).asJsonObject
                        if (firstPrompt == null && obj.get("type")?.asString == "user") {
                            val msg = obj.getAsJsonObject("message")
                            if (msg != null) {
                                val content = msg.get("content")
                                firstPrompt = when {
                                    content == null || content.isJsonNull -> null
                                    content.isJsonPrimitive -> content.asString
                                    content.isJsonArray -> content.asJsonArray
                                        .firstOrNull { it.isJsonObject && it.asJsonObject.get("type")?.asString == "text" }
                                        ?.asJsonObject?.get("text")?.asString
                                    else -> null
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Read tail for customTitle (set by /rename)
        customTitle = readCustomTitle(file)

        return ClaudeSession(id, customTitle, file.lastModified(), firstPrompt)
    }

    private fun readCustomTitle(file: File): String? {
        try {
            val tailLines = readTail(file, 4096)
            for (line in tailLines.asReversed()) {
                try {
                    val obj = JsonParser.parseString(line).asJsonObject
                    if (obj.get("type")?.asString == "custom-title") {
                        return obj.get("customTitle")?.asString
                    }
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun readTail(file: File, bytes: Int): List<String> {
        RandomAccessFile(file, "r").use { raf ->
            val fileLen = raf.length()
            val start = maxOf(0L, fileLen - bytes)
            raf.seek(start)
            val buf = ByteArray((fileLen - start).toInt())
            raf.readFully(buf)
            val text = String(buf, Charsets.UTF_8)
            val lines = text.lines().filter { it.isNotBlank() }
            // If we started mid-line, drop the first (partial) line
            return if (start > 0 && lines.isNotEmpty()) lines.drop(1) else lines
        }
    }

    private fun parseIsoTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}
