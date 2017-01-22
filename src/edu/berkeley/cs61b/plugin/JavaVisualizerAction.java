package edu.berkeley.cs61b.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

public class JavaVisualizerAction extends AnAction {
	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getProject();
		if (project != null) {
			XDebugSession debugSession = XDebuggerManager.getInstance(project).getCurrentSession();
			if (debugSession != null) {
				JavaVisualizerController controller = JavaVisualizerController.findOrAttachVisualizer(debugSession);
				debugSession.getUI().selectAndFocus(controller.getContent(), true, true);
			}
		}
	}
}
