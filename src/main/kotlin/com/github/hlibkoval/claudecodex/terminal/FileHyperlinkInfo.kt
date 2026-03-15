package com.github.hlibkoval.claudecodex.terminal

import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

class FileHyperlinkInfo(
    private val absolutePath: String,
    private val localFileSystem: LocalFileSystem,
    private val startLine: Int?,
    private val endLine: Int?,
) : HyperlinkInfo {

    override fun navigate(project: Project) {
        try {
            ApplicationManager.getApplication().invokeLater {
                val vf = localFileSystem.findFileByPath(absolutePath) ?: return@invokeLater
                val line = if (startLine != null && startLine > 0) startLine - 1 else -1
                val descriptor = OpenFileDescriptor(project, vf, line, 0)
                descriptor.navigate(true)

                if (endLine != null && startLine != null && endLine > startLine) {
                    val editor = FileEditorManager.getInstance(project).selectedTextEditor
                        ?: return@invokeLater
                    val doc = editor.document
                    val startIdx = (startLine - 1).coerceIn(0, doc.lineCount - 1)
                    val endIdx = (endLine - 1).coerceIn(0, doc.lineCount - 1)
                    editor.selectionModel.setSelection(
                        doc.getLineStartOffset(startIdx),
                        doc.getLineEndOffset(endIdx),
                    )
                }
            }
        } catch (_: Exception) {
        }
    }
}
