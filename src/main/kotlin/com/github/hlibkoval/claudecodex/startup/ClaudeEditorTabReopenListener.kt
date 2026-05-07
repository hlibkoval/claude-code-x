package com.github.hlibkoval.claudecodex.startup

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class ClaudeEditorTabReopenListener(private val project: Project) : FileEditorManagerListener {

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file.getUserData(ClaudeEditorTabKeys.CLAUDE_TAB_MARKER) != true) return
        if (file.getUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN) == true) return
        if (project.isDisposed || !project.isOpen) return

        val props = PropertiesComponent.getInstance(project)
        props.setValue(ClaudeEditorTabKeys.PROP_USER_CLOSED_THIS_SESSION, true)
        props.setValue(ClaudeEditorTabKeys.PROP_LAST_OPEN_INTENT, false)
    }
}
