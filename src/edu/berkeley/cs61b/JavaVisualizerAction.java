package edu.berkeley.cs61b;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;

import javax.swing.JComponent;
import javax.swing.JLabel;

public class JavaVisualizerAction extends AnAction {
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";

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
		return new JLabel("insert some content here");
	}

	private XDebugSession getDebugSession(Project project) {
		return XDebuggerManager.getInstance(project).getCurrentSession();
	}
}
