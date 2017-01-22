package edu.berkeley.cs61b;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;

import javax.swing.JComponent;

public class JavaVisualizerAction extends AnAction {
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";

	private JComponent component = null;

	@Override
	public void actionPerformed(AnActionEvent e) {
		System.out.println("Java Visualizer");

		Project project = e.getProject();
		if (project != null) {
			getDebugSession(project).getUI().selectAndFocus(findContent(project), true, true);
		}
	}

	private Content findContent(Project project) {
		XDebugSession session = getDebugSession(project);
		if (session != null) {
			RunnerLayoutUi ui = session.getUI();
			Content content = ui.findContent(CONTENT_ID);
			if (content != null) {
				return content;
			} else {
				content = ui.createContent(CONTENT_ID, createContent(), "Java Visualizer", IconLoader.getIcon("/icons/hug.png"), null);
				ui.addContent(content);
				return content;
			}
		} else {
			return null;
		}
	}

	private JComponent createContent() {
		if (component != null) {
			return component;
		}

		final JFXPanel jfxPanel = new JFXPanel();
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			WebView webView = new WebView();
			jfxPanel.setScene(new Scene(webView));
			webView.getEngine().load(getClass().getResource("/web/visualizer.html").toExternalForm());
		});
		return (this.component = jfxPanel);
	}

	private XDebugSession getDebugSession(Project project) {
		return XDebuggerManager.getInstance(project).getCurrentSession();
	}
}
