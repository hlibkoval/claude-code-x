package com.github.hlibkoval.claudecodex.startup

import com.intellij.openapi.util.Key

internal object ClaudeEditorTabKeys {
    val CLAUDE_TAB_MARKER: Key<Boolean> = Key.create("claudeCodeX.claudeTabMarker")
    val CLAUDE_AGENTS_TAB_MARKER: Key<Boolean> = Key.create("claudeCodeX.claudeAgentsTabMarker")

    const val PROP_USER_CLOSED_THIS_SESSION = "claudeCodeX.editorTab.userClosedThisSession"
    const val PROP_LAST_OPEN_INTENT = "claudeCodeX.editorTab.lastOpenIntent"
}
