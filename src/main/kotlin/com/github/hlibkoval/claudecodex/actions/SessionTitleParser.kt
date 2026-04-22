package com.github.hlibkoval.claudecodex.actions

/**
 * Derives a concise, human-readable display title from the raw first-user-message
 * content of a Claude Code session. See src/test/resources/session-title-samples.txt
 * for the full fixture set the parser is validated against.
 *
 * Pipeline:
 *   1. Trim.
 *   2. Strip "noise" tag blocks (`<system-reminder>`, `<task-notification>`,
 *      `<local-command-caveat>`) as whole blocks — tag, body, closing tag all gone.
 *   3. If what remains is a pure slash-command block (only `<command-name>`,
 *      `<command-message>`, `<command-args>` tags separated by whitespace),
 *      format it as `/name args`.
 *   4. Otherwise, strip every remaining opening and closing XML-like tag marker
 *      (including partial/unmatched) with a space, leaving inner text intact.
 *   5. Collapse runs of whitespace to a single space and trim.
 *
 * The parser never throws, never returns null, and is idempotent:
 * `parse(parse(x)) == parse(x)` for all inputs.
 */
object SessionTitleParser {

    private val NOISE_TAGS = listOf("system-reminder", "task-notification", "local-command-caveat")

    private val OPENING_TAG = Regex("<[a-zA-Z][a-zA-Z0-9\\-]*(?:\\s[^>]*)?>")
    private val CLOSING_TAG = Regex("</[a-zA-Z][a-zA-Z0-9\\-]*\\s*>")
    private val WHITESPACE_RUN = Regex("\\s+")

    private val NOISE_BLOCKS: List<Regex> = NOISE_TAGS.map { tag ->
        Regex("<$tag(?:\\s[^>]*)?>.*?</$tag\\s*>", RegexOption.DOT_MATCHES_ALL)
    }

    private val ANY_COMMAND_BLOCK = Regex(
        "<command-(?:name|message|args)(?:\\s[^>]*)?>.*?</command-(?:name|message|args)\\s*>",
        RegexOption.DOT_MATCHES_ALL
    )
    private val COMMAND_NAME_RE = Regex(
        "<command-name(?:\\s[^>]*)?>(.*?)</command-name\\s*>",
        RegexOption.DOT_MATCHES_ALL
    )
    private val COMMAND_ARGS_RE = Regex(
        "<command-args(?:\\s[^>]*)?>(.*?)</command-args\\s*>",
        RegexOption.DOT_MATCHES_ALL
    )

    fun parse(raw: String): String {
        var text = raw.trim()
        if (text.isEmpty()) return ""

        for (block in NOISE_BLOCKS) {
            text = block.replace(text, " ")
        }
        text = text.trim()
        if (text.isEmpty()) return ""

        val remainder = ANY_COMMAND_BLOCK.replace(text, "").trim()
        if (remainder.isEmpty()) {
            val name = COMMAND_NAME_RE.find(text)?.groupValues?.get(1)?.trim().orEmpty()
            if (name.isNotEmpty()) {
                val args = COMMAND_ARGS_RE.find(text)?.groupValues?.get(1)?.trim().orEmpty()
                val prefixed = if (name.startsWith("/")) name else "/$name"
                val combined = if (args.isEmpty()) prefixed else "$prefixed $args"
                return WHITESPACE_RUN.replace(combined, " ").trim()
            }
        }

        text = OPENING_TAG.replace(text, " ")
        text = CLOSING_TAG.replace(text, " ")
        return WHITESPACE_RUN.replace(text, " ").trim()
    }
}
