package edu.berkeley.cs61b;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;

public class CheckStyleAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = event.getData(PlatformDataKeys.PROJECT);
		VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);

		if (project != null && file != null) {
			// get the Style Checker ToolWindow
			final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Style Checker");
			toolWindow.activate(() -> {
				// upon activation, print some stuff out.
				ContentManager contentManager = toolWindow.getContentManager();
				Content content = contentManager.findContent("");
				ConsoleView consoleView = (ConsoleView) content.getComponent();

				consoleView.print(file.getPath(), ConsoleViewContentType.ERROR_OUTPUT);
			});
		}
	}
}
