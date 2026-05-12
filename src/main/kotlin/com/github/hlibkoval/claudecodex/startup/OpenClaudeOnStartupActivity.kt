package com.github.hlibkoval.claudecodex.startup

import com.github.hlibkoval.claudecodex.actions.ClaudeTerminalUtil
import com.github.hlibkoval.claudecodex.settings.ClaudeCodeXSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class OpenClaudeOnStartupActivity : ProjectActivity {
    init {
        val app = ApplicationManager.getApplication()
        if (app.isCommandLine || app.isHeadlessEnvironment || app.isUnitTestMode) {
            throw ExtensionNotApplicableException.create()
        }
    }

    override suspend fun execute(project: Project) {
        val settings = ClaudeCodeXSettings.getInstance(project)
        if (!settings.openOnStartup) return

        val props = PropertiesComponent.getInstance(project)
        props.unsetValue(ClaudeEditorTabKeys.PROP_USER_CLOSED_THIS_SESSION)
        if (!props.getBoolean(ClaudeEditorTabKeys.PROP_LAST_OPEN_INTENT, true)) return

        if (settings.useAgentsMode) {
            withContext(Dispatchers.EDT) {
                ClaudeTerminalUtil.openAgentsView(project, requestFocus = false)
            }
            return
        }

        val previousOpenInEditor = settings.openInEditor
        settings.openInEditor = true
        try {
            withContext(Dispatchers.EDT) {
                ClaudeTerminalUtil.openSession(project, requestFocus = false)
            }
        } finally {
            settings.openInEditor = previousOpenInEditor
        }
    }
}
