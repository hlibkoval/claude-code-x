package com.github.hlibkoval.claudeext.services

import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class ClaudeSessionService(private val project: Project) {

    data class ClaudeSession(
        val id: String,
        val slug: String?,
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
                val sessions = parseSessionsIndex(indexFile)
                if (sessions.isNotEmpty()) return sessions
            } catch (_: Exception) {
            }
        }

        return dir.listFiles { f -> f.extension == "jsonl" }
            ?.map { parseJsonlSession(it) }
            ?.sortedByDescending { it.modified }
            ?: emptyList()
    }

    private fun parseSessionsIndex(file: File): List<ClaudeSession> {
        val json = JsonParser.parseString(file.readText())
        if (!json.isJsonArray) return emptyList()
        return json.asJsonArray.mapNotNull { element ->
            if (!element.isJsonObject) return@mapNotNull null
            val obj = element.asJsonObject
            val sessionId = obj.get("sessionId")?.asString ?: return@mapNotNull null
            val firstPrompt = obj.get("firstPrompt")?.asString
            val modified = obj.get("modified")?.asLong ?: 0L
            ClaudeSession(sessionId, null, modified, firstPrompt)
        }.sortedByDescending { it.modified }
    }

    private fun parseJsonlSession(file: File): ClaudeSession {
        val id = file.nameWithoutExtension
        var slug: String? = null
        var firstPrompt: String? = null
        try {
            BufferedReader(FileReader(file)).use { reader ->
                var linesRead = 0
                var line: String?
                while (reader.readLine().also { line = it } != null && linesRead < 20) {
                    linesRead++
                    try {
                        val obj = JsonParser.parseString(line).asJsonObject
                        if (slug == null) {
                            slug = obj.get("slug")?.takeIf { !it.isJsonNull }?.asString
                        }
                        if (firstPrompt == null) {
                            val type = obj.get("type")?.asString
                            val role = obj.get("role")?.asString
                            if (type == "user" || role == "user") {
                                firstPrompt = obj.get("message")?.asString
                                    ?: obj.get("content")?.asString
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
        return ClaudeSession(id, slug, file.lastModified(), firstPrompt)
    }
}
