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
import com.intellij.openapi.util.Key;
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

public class JavaVisualizerController {
	private static final String CONTENT_ID = "61B.JavaVisualizerContent";
	private static final Key<JavaVisualizerController> CONTROLLER_KEY = Key.create("JavaVisualizerController");

	private Project project;
	private XDebugSession session;
	private JComponent component;
	private WebView webView;
	private boolean webViewReady;
	private JDI2JSON jdi2json;
	private Content content;

	private JavaVisualizerController(XDebugSession session) {
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

	static JavaVisualizerController findOrAttachVisualizer(XDebugSession session) {
		if (session != null) {
			RunnerLayoutUi ui = session.getUI();
			Content content = ui.findContent(CONTENT_ID);
			JavaVisualizerController controller;

			if (content != null) {
				controller = content.getUserData(CONTROLLER_KEY);
			} else {
				controller = new JavaVisualizerController(session);
				controller.content = ui.createContent(
						CONTENT_ID,
						controller.component,
						"Java Visualizer",
						IconLoader.getIcon("/icons/hug.png"),
						null);
				controller.content.putUserData(CONTROLLER_KEY, controller);
				ui.addContent(controller.content);
			}

			return controller;
		} else {
			return null;
		}
	}

	public Content getContent() {
		return content;
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

	public void forceRefreshVisualizer() {
		System.out.println("Force refreshing visualizer...");
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
			if (objs.size() == 0) {
				// nothing to show here.
				return;
			}

			JsonArrayBuilder arr = Json.createArrayBuilder();
			objs.forEach(arr::add);
			String trace = arr.build().toString();

			Platform.runLater(() -> {
				if (webView != null && webViewReady) {
					JSObject window = (JSObject) webView.getEngine().executeScript("window");
					window.call("visualize", trace);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
