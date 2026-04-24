package com.github.hlibkoval.claudecodex.actions;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.view.TerminalView;

public final class TerminalViewVirtualFileFactory {
    public static VirtualFile create(TerminalView view, boolean closeOnProcessTermination) {
        return new TerminalViewVirtualFile(view, closeOnProcessTermination);
    }

    private TerminalViewVirtualFileFactory() {}
}
