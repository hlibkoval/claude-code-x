package com.github.hlibkoval.claudecodex.actions;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.view.TerminalView;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public final class TerminalViewVirtualFileFactory {
    public static VirtualFile create(TerminalView view) {
        return new TerminalViewVirtualFile(view);
    }

    @Nullable
    public static TerminalView getTerminalView(VirtualFile file) {
        if (file instanceof TerminalViewVirtualFile) {
            return ((TerminalViewVirtualFile) file).getTerminalView();
        }
        return null;
    }

    /**
     * Uses reflection to avoid resolving StateFlow in our classloader,
     * which conflicts with the platform's kotlinx-coroutines classloader.
     */
    public static boolean isSessionRunning(TerminalView view) {
        try {
            Method getSessionState = view.getClass().getMethod("getSessionState");
            Object stateFlow = getSessionState.invoke(view);
            Method getValue = stateFlow.getClass().getMethod("getValue");
            getValue.setAccessible(true);
            Object state = getValue.invoke(stateFlow);
            LOG.info("Terminal session state: " + state.getClass().getName() + " (simpleName=" + state.getClass().getSimpleName() + ")");
            return state.getClass().getSimpleName().equals("Running");
        } catch (Throwable e) {
            LOG.warn("Failed to check terminal session state", e);
            return false;
        }
    }

    private static final Logger LOG = Logger.getInstance(TerminalViewVirtualFileFactory.class);

    @Nullable
    public static String getTabName(TerminalView view) {
        return view.getTitle().getDefaultTitle();
    }

    private TerminalViewVirtualFileFactory() {}
}
