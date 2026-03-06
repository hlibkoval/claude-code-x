package com.github.hlibkoval.claudecodex.listeners

import com.github.hlibkoval.claudecodex.actions.CLAUDE_TERMINAL_KEY
import com.github.hlibkoval.claudecodex.actions.TerminalViewVirtualFileFactory
import com.intellij.CommonBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class ClaudeTerminalCloseListener(private val project: Project) : FileEditorManagerListener.Before {

    override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
        if (project.isDisposed) return
        if (file.getUserData(CLAUDE_TERMINAL_KEY) != true) return

        val view = TerminalViewVirtualFileFactory.getTerminalView(file) ?: return
        if (!TerminalViewVirtualFileFactory.isSessionRunning(view)) return

        val tabName = TerminalViewVirtualFileFactory.getTabName(view) ?: "Claude Code"
        val result = Messages.showDialog(
            project,
            "Do you want to terminate the process 'Terminal $tabName'?",
            "Process 'Terminal $tabName' Is Running",
            arrayOf("Terminate", CommonBundle.getCancelButtonText()),
            1,
            Messages.getWarningIcon()
        )

        if (result != 0) {
            // User cancelled — preserve terminal and reopen
            file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                val newFile = TerminalViewVirtualFileFactory.create(view)
                newFile.putUserData(CLAUDE_TERMINAL_KEY, true)
                newFile.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
                try {
                    FileEditorManager.getInstance(project).openFile(newFile, true)
                } finally {
                    newFile.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, null)
                }
            }
        }
    }
}
