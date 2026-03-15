package com.github.hlibkoval.claudecodex.terminal

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Short-lived cache for file existence checks to avoid repeated stat() calls
 * during terminal output processing. Entries expire after [ttlMs] milliseconds.
 */
class FileExistenceCache(private val ttlMs: Long = 5_000) {

    private data class CacheEntry(val exists: Boolean, val timestamp: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun isFile(path: String): Boolean {
        val now = System.currentTimeMillis()
        val entry = cache[path]
        if (entry != null && now - entry.timestamp < ttlMs) {
            return entry.exists
        }
        val exists = File(path).isFile
        cache[path] = CacheEntry(exists, now)
        // Lazy cleanup: remove stale entries when cache grows
        if (cache.size > 500) {
            cache.entries.removeIf { now - it.value.timestamp > ttlMs }
        }
        return exists
    }
}
