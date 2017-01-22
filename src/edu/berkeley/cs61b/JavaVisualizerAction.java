package edu.berkeley.cs61b;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.sun.jdi.Location;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import traceprinter.JDI2JSON;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.swing.JComponent;
import java.util.ArrayList;

public class JavaVisualizerAction extends AnAction{
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";

	private JComponent component = null;
	private WebView webView = null;

	@Override
	public void actionPerformed(AnActionEvent e) {
		System.out.println("Java Visualizer");

		Project project = e.getProject();
		if (project != null) {

			XDebugSession debugSession = getDebugSession(project);
			debugSession.getUI().selectAndFocus(findContent(project), true, true);
			DebugProcess debugProcess = DebuggerManager.getInstance(project).getDebugProcess(debugSession.getDebugProcess().getProcessHandler());
			debugProcess.addDebugProcessListener(new DebugProcessListener() {
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
			webView = new WebView();
			jfxPanel.setScene(new Scene(webView));
			webView.getEngine().load(getClass().getResource("/web/visualizer.html").toExternalForm());
		});
		return (this.component = jfxPanel);
	}

	private XDebugSession getDebugSession(Project project) {
		return XDebuggerManager.getInstance(project).getCurrentSession();
	}

	private void visualizeSuspendContext(SuspendContext suspendContext) {
		try {
			JDI2JSON jdi2json = new JDI2JSON(null,
					System.in,
					System.in,
					null);

			Location l = suspendContext.getThread().frame(suspendContext.getThread().frameCount() - 1).location();
			ArrayList<JsonObject> objs = jdi2json.convertExecutionPoint(null, l, suspendContext.getThread().getThreadReference());

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
				((JSObject) webView.getEngine().executeScript("visualizer")).call("visualize", json);
			}
		});
	}
}
