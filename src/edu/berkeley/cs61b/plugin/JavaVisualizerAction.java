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
	@Override
	public void actionPerformed(AnActionEvent e) {
		Project project = e.getProject();
		if (project != null) {
			XDebugSession debugSession = XDebuggerManager.getInstance(project).getCurrentSession();
			if (debugSession != null) {
				Content content = JavaVisualizerWindow.findOrAttachVisualizer(debugSession);
				debugSession.getUI().selectAndFocus(content, true, true);
				// todo trigger refresh
			}
		}
	}
}
