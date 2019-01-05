package edu.berkeley.cs61b.plugin;

import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CheckStyleAction extends AnAction {
	private static final String CONFIG_ROOT = "style_config/";

	@Override
	public void update(AnActionEvent event) {
		super.update(event);

		// only show if selecting a directory or .java file (or multiple)
		Project project = event.getData(PlatformDataKeys.PROJECT);
		VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

		boolean showAction = false;
		if (project != null && files != null) {
			for (VirtualFile file : files) {
				if (file.isDirectory() || "java".equals(file.getExtension())) {
					showAction = true;
					break;
				}
			}
		}
		event.getPresentation().setVisible(showAction);
	}

	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = event.getData(PlatformDataKeys.PROJECT);
		VirtualFile[] inputFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

		if (project != null && inputFiles != null) {
			// get the Style Checker ToolWindow
			final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Style Checker");
			toolWindow.activate(() -> {
				// upon activation, print some stuff out.
				ContentManager contentManager = toolWindow.getContentManager();
				Content content = contentManager.findContent("");
				ConsoleView consoleView = content.getUserData(CheckStyleToolWindowFactory.KEY_CONSOLE);

				// iteratively add all files to a list
				List<File> checkerFiles = new ArrayList<>();
				collectFiles(inputFiles, checkerFiles);
				checkerFiles.removeIf(p -> !p.getName().endsWith(".java"));

				consoleView.clear();
				String message = String.format("Running style checker on %d file(s) ", checkerFiles.size());
				consoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT);
				runCheckStyle(project, consoleView, checkerFiles);
			});
		}
	}

	private void collectFiles(VirtualFile[] parent, List<File> list) {
		LinkedList<VirtualFile> sources = new LinkedList<>();
		Collections.addAll(sources, parent);

		while (sources.size() > 0) {
			VirtualFile f = sources.removeFirst();
			if (f.isDirectory()) {
				Collections.addAll(sources, f.getChildren());
			} else {
				list.add(new File(f.getPath()));
			}
		}
	}

	/**
	 * Determines the correct checks XML and suppressions XML to use based on the semester.
	 * Reads from CONFIG_ROOT/index.txt
	 *
	 * @return a String array with: {config name, checks XML, suppressions XML}
	 */
	private String[] pickConfig(String semester) throws IOException {
		InputStream indexStream = getClass().getClassLoader().getResourceAsStream(CONFIG_ROOT + "index.txt");
		if (indexStream == null) {
			throw new IOException("style check config index file not found");
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(indexStream));
		String line;
		while ((line = r.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] data = line.split("\t");
			if (semester.matches(data[0])) {
				return new String[]{semester, CONFIG_ROOT + data[1], CONFIG_ROOT + data[2]};
			}
		}
		throw new RuntimeException("No config file found for semester: " + semester);
	}

	private void runCheckStyle(Project project, ConsoleView consoleView, List<File> files) {

		Configuration config;
		String configName;
		try {
			String[] configFiles = pickConfig(PluginUtils.getSemesterID());
			configName = configFiles[0];
			String configChecks = configFiles[1];
			String configSuppressions = configFiles[2];

			System.setProperty("checkstyle.suppress.file", getClass().getClassLoader().getResource(configSuppressions).toString());

			PropertiesExpander properties = new PropertiesExpander(System.getProperties());
			InputSource configSource = new InputSource(getClass().getClassLoader().getResourceAsStream(configChecks));

			config = ConfigurationLoader.loadConfiguration(configSource, properties, true);
		} catch (Exception e) {
			consoleView.print("Error loading style checker config: " + e.getMessage() + "\n", ConsoleViewContentType.ERROR_OUTPUT);
			e.printStackTrace();
			return;
		}

		consoleView.print("(config: " + configName + ")...\n", ConsoleViewContentType.SYSTEM_OUTPUT);
		try {
			Checker c = new Checker();
			ClassLoader e = Checker.class.getClassLoader();
			c.setModuleClassLoader(e);
			c.configure(config);
			c.addListener(new LoggingAuditListener(project, consoleView));

			int numErrs = c.process(files);
			c.destroy();
			if (numErrs > 0) {
				consoleView.print("Style checker completed with " + numErrs + " errors." + "\n", ConsoleViewContentType.ERROR_OUTPUT);
			} else {
				consoleView.print("Style checker completed with no errors.", ConsoleViewContentType.SYSTEM_OUTPUT);
			}
		} catch (Throwable e) {
			consoleView.print("Error running style checker: " + e.getMessage() + "\n", ConsoleViewContentType.ERROR_OUTPUT);
			e.printStackTrace();
		}
	}

	private class LoggingAuditListener implements AuditListener {
		private ConsoleView console;
		private Project project;

		LoggingAuditListener(Project project, ConsoleView console) {
			this.project = project;
			this.console = console;
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
			VirtualFile f = LocalFileSystem.getInstance().findFileByPath(e.getFileName());
			if (f != null) {
				Path file_path = Paths.get(f.getPath());
				Path base_path = Paths.get(project.getBasePath());
				String display_path;

				try {
					display_path = base_path.relativize(file_path).toString();
				} catch (IllegalArgumentException e1) {
					// 'cannot be made relative'
					display_path = file_path.toString();
				}

				String linkText = display_path + ":" + e.getLine();
				if (e.getColumn() != 0) {
					linkText += ":" + e.getColumn();
				}
				console.printHyperlink(linkText, new OpenFileHyperlinkInfo(project, f, e.getLine() - 1, e.getColumn()));
			} else {
				console.print(e.getFileName() + ":" + e.getLine(), ConsoleViewContentType.NORMAL_OUTPUT);
			}
			console.print(": " + e.getMessage() + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
		}

		@Override
		public void addException(AuditEvent e, Throwable throwable) {
		}
	}
}
