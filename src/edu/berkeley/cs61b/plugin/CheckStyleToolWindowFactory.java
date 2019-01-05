package edu.berkeley.cs61b.plugin;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class CheckStyleToolWindowFactory implements ToolWindowFactory {
	static final Key<ConsoleView> KEY_CONSOLE = Key.create("style output console");

	@Override
	public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
		SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);

		final ActionManager actionManager = ActionManager.getInstance();
		ActionToolbar actionToolbar = actionManager.createActionToolbar(
				"61B.CheckStyleToolbar",
				(DefaultActionGroup) actionManager.getAction("61B.CheckStyleToolbar"),
				false
		);
		panel.setToolbar(actionToolbar.getComponent());

		ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
		panel.setContent(consoleView.getComponent());

		Content content = toolWindow.getContentManager().getFactory().createContent(panel, "", true);
		content.putUserData(KEY_CONSOLE, consoleView);
		toolWindow.getContentManager().addContent(content);
	}
}