package edu.berkeley.cs61b.plugin;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.SuspendContext;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
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

public class JavaVisualizerWindow {
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";

	private Project project;
	private XDebugSession session;
	private JComponent component;
	private WebView webView;
	private boolean webViewReady;
	private JDI2JSON jdi2json;

	static Content findOrAttachVisualizer(XDebugSession session) {
		if (session != null) {
			RunnerLayoutUi ui = session.getUI();
			Content content = ui.findContent(CONTENT_ID);
			if (content != null) {
				return content;
			} else {
				JavaVisualizerWindow window = new JavaVisualizerWindow(session);
				content = ui.createContent(
						CONTENT_ID,
						window.component,
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

	private JavaVisualizerWindow(XDebugSession session) {
		this.session = session;
		this.project = session.getProject();
		this.component = createComponent();
		this.jdi2json = new JDI2JSON();

		getDebugProcess().addDebugProcessListener(new DebugProcessListener() {
			@Override
			public void paused(SuspendContext suspendContext) {
				visualizeSuspendContext(suspendContext);
			}
		});
		if (webViewReady) {
			forceRefreshVisualizer();
		}
	}

	private DebugProcess getDebugProcess() {
		return DebuggerManager.getInstance(project).getDebugProcess(session.getDebugProcess().getProcessHandler());
	}


	private JComponent createComponent() {
		final JFXPanel jfxPanel = new JFXPanel();
		Platform.setImplicitExit(false);
		Platform.runLater(() -> {
			webView = new WebView();
			webView.getEngine().setOnStatusChanged((WebEvent<String> e) -> {
				if (e != null && e.getData() != null && e.getData().equals("Ready")) {
					webViewReady = true;
					forceRefreshVisualizer();
				}
			});
			webView.getEngine().load(getClass().getResource("/web/visualizer.html").toExternalForm());
			jfxPanel.setScene(new Scene(webView));
		});
		return (this.component = jfxPanel);
	}

	private void forceRefreshVisualizer() {
		getDebugProcess().getManagerThread().invokeCommand(new DebuggerCommand() {
			@Override
			public void action() {
				visualizeSuspendContext(getSuspendContext());
			}

			@Override
			public void commandCancelled() {
			}
		});
	}

	private SuspendContext getSuspendContext() {
		// this *definitely* violates some abstraction barriers.
		// works in practice though.
		return ((SuspendContextImpl) session.getSuspendContext());
	}

	private void visualizeSuspendContext(SuspendContext sc) {
		try {
			ArrayList<JsonObject> objs = jdi2json.convertExecutionPoint(sc.getThread().getThreadReference());
			JsonArrayBuilder arr = Json.createArrayBuilder();
			for (JsonObject obj : objs) {
				arr.add(obj);
			}
			String trace = arr.build().toString();

			Platform.runLater(() -> {
				if (webView != null) {
					((JSObject) webView.getEngine().executeScript("window")).call("visualize", trace);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
