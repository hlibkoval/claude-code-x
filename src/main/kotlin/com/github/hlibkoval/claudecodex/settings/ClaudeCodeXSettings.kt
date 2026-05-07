package com.github.hlibkoval.claudecodex.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "ClaudeCodeXSettings", storages = [Storage("claudeCodeX.xml")])
class ClaudeCodeXSettings : SimplePersistentStateComponent<ClaudeCodeXSettings.State>(State()) {

    class State : BaseState() {
        var openInEditor by property(false)
        var openOnStartup by property(false)
    }

    var openInEditor: Boolean
        get() = state.openInEditor
        set(value) { state.openInEditor = value }

    var openOnStartup: Boolean
        get() = state.openOnStartup
        set(value) { state.openOnStartup = value }

    companion object {
        fun getInstance(project: Project): ClaudeCodeXSettings =
            project.getService(ClaudeCodeXSettings::class.java)
    }
}
