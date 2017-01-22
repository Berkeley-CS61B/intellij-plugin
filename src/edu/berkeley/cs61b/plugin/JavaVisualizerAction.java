package edu.berkeley.cs61b.plugin;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
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
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import traceprinter.JDI2JSON;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.swing.JComponent;
import java.util.ArrayList;

public class JavaVisualizerAction extends AnAction {
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";

	private JComponent component = null;
	private WebView webView = null;

	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getProject();
		if (project != null) {
			XDebugSession debugSession = getDebugSession(project);
			debugSession.getUI().selectAndFocus(findContent(project), true, true);
			getDebugProcess(debugSession).addDebugProcessListener(new DebugProcessListener() {
				@Override
				public void paused(SuspendContext suspendContext) {
					visualizeSuspendContext(suspendContext);
				}
			});
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
				content = ui.createContent(
						CONTENT_ID,
						createContent(project),
						"Java Visualizer",
						IconLoader.getIcon("/icons/hug.png"),
						null);
				ui.addContent(content);
				return content;
			}
		} else {
			return null;
		}
	}

	private JComponent createContent(Project project) {
		if (component != null) {
			return component;
		}

		final JFXPanel jfxPanel = new JFXPanel();
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			webView = new WebView();
			jfxPanel.setScene(new Scene(webView));
			webView.getEngine().setOnStatusChanged((WebEvent<String> e) -> {
				// sshh let's just pretend this is 'onReady'
				refreshVisualizer(project);
			});
			webView.getEngine().load(getClass().getResource("/web/visualizer.html").toExternalForm());
		});
		return (this.component = jfxPanel);
	}

	private XDebugSession getDebugSession(Project project) {
		return XDebuggerManager.getInstance(project).getCurrentSession();
	}

	private DebugProcess getDebugProcess(XDebugSession debugSession) {
		return DebuggerManager
				.getInstance(debugSession.getProject())
				.getDebugProcess(debugSession.getDebugProcess().getProcessHandler());
	}

	private SuspendContext getSuspendContext(XDebugSession session) {
		// this *definitely* violates some abstraction barriers.
		// works in practice though.
		return ((SuspendContextImpl) session.getSuspendContext());
	}

	private void visualizeSuspendContext(SuspendContext sc) {
		try {
			JDI2JSON jdi2json = new JDI2JSON(null,
					System.in,
					System.in,
					null);

			ArrayList<JsonObject> objs = jdi2json.convertExecutionPoint(sc.getThread().getThreadReference());
			JsonArrayBuilder arr = Json.createArrayBuilder();
			for (JsonObject obj : objs) {
				arr.add(obj);
			}
			String trace = arr.build().toString();
			updateVisualizer(trace);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void updateVisualizer(String json) {
		Platform.runLater(() -> {
			if (webView != null) {
				((JSObject) webView.getEngine().executeScript("window")).call("visualize", json);
			}
		});
	}

	private void refreshVisualizer(Project project) {
		XDebugSession debugSession = getDebugSession(project);
		getDebugProcess(debugSession).getManagerThread().invokeCommand(new DebuggerCommand() {
			@Override
			public void action() {
				visualizeSuspendContext(getSuspendContext(debugSession));
			}

			@Override
			public void commandCancelled() {
			}
		});
	}
}
