package edu.berkeley.cs61b;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

public class CheckStyleAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent event) {
		Project project = event.getData(PlatformDataKeys.PROJECT);
		VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
		if (file != null) {
			Messages.showMessageDialog(project, file.getPath(), "File Info", Messages.getInformationIcon());
		}
	}
}
