package com.github.hlibkoval.claudecodex.listeners

import com.github.hlibkoval.claudecodex.actions.CLAUDE_TERMINAL_KEY
import com.github.hlibkoval.claudecodex.actions.TerminalViewVirtualFileFactory
import com.intellij.CommonBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.fileEditor.impl.FileEditorOpenOptions
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class ClaudeTerminalCloseListener(private val project: Project) : FileEditorManagerListener.Before {

    override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
        if (project.isDisposed) return
        if (file.getUserData(CLAUDE_TERMINAL_KEY) != true) return
        if (file.getUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN) == true) return

        val view = TerminalViewVirtualFileFactory.getTerminalView(file) ?: return
        if (!TerminalViewVirtualFileFactory.isSessionRunning(view)) return

        // Capture position before the close proceeds
        val managerEx = FileEditorManagerEx.getInstanceEx(project)
        val window = managerEx.windows.firstOrNull { it.isFileOpen(file) }
        val tabIndex = window?.fileList?.indexOf(file) ?: -1

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
            // User cancelled — preserve terminal and reopen in same position
            file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                val newFile = TerminalViewVirtualFileFactory.create(view)
                newFile.putUserData(CLAUDE_TERMINAL_KEY, true)
                newFile.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, true)
                try {
                    reopenInPlace(managerEx, newFile, window, tabIndex)
                } finally {
                    newFile.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, null)
                }
            }
        }
    }

    private fun reopenInPlace(
        managerEx: FileEditorManagerEx,
        file: VirtualFile,
        window: EditorWindow?,
        tabIndex: Int
    ) {
        if (window != null && window.tabCount > 0) {
            val options = FileEditorOpenOptions(
                requestFocus = true,
                index = if (tabIndex >= 0) tabIndex else -1
            )
            managerEx.openFile(file, window, options)
        } else {
            managerEx.openFile(file, true)
        }
    }
}
