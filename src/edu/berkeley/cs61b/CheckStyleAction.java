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
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.xml.sax.InputSource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

				List<File> files = new ArrayList<>();
				files.add(new File(file.getPath()));

				consoleView.print("Running style checker on " + file.getPath() + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
				runCheckStyle(consoleView, files);
			});
		}
	}

	private void runCheckStyle(ConsoleView consoleView, List<File> files) {
		Configuration config;
		try {
			PropertiesExpander properties = new PropertiesExpander(System.getProperties());
			InputSource configSource = new InputSource(getClass().getClassLoader().getResourceAsStream("61b_checks.xml"));
			config = ConfigurationLoader.loadConfiguration(configSource, properties, true);
		} catch (Exception e) {
			consoleView.print("Unable to start style checker (1): " + e.getMessage() + "\n", ConsoleViewContentType.ERROR_OUTPUT);
			return;
		}

		try {
			Checker c = new Checker();
			ClassLoader e = Checker.class.getClassLoader();
			c.setModuleClassLoader(e);
			c.configure(config);
			c.addListener(new LoggingAuditListener(consoleView));

			int numErrs = c.process(files);
			c.destroy();
			consoleView.print("Style checker completed with " + numErrs + " errors" + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);
		} catch (Exception e) {
			consoleView.print("Unable to start style checker (2): " + e.getMessage() + "\n", ConsoleViewContentType.ERROR_OUTPUT);
			e.printStackTrace();
		}
	}

	private class LoggingAuditListener implements AuditListener {
		private ConsoleView consoleView;

		LoggingAuditListener(ConsoleView consoleView) {
			this.consoleView = consoleView;
		}

		private void println(String str) {
			consoleView.print(str + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
		}

		@Override
		public void auditStarted(AuditEvent e) {
		}

		@Override
		public void auditFinished(AuditEvent e) {
		}

		@Override
		public void fileStarted(AuditEvent e) {
		}

		@Override
		public void fileFinished(AuditEvent e) {
		}

		@Override
		public void addError(AuditEvent e) {
			String out = String.format("%s:%d:%d: %s",
					e.getFileName(),
					e.getLine(),
					e.getColumn(),
					e.getMessage()
			);
			println(out);
		}

		@Override
		public void addException(AuditEvent e, Throwable throwable) {
			println("exception");
		}
	}
}
