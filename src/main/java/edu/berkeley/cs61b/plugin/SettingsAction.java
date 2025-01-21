package edu.berkeley.cs61b.plugin;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;

public class SettingsAction extends AnAction {
	@Override
	public void actionPerformed(@NotNull AnActionEvent e) {
		PropertiesComponent props = PropertiesComponent.getInstance();

		SettingsForm form = new SettingsForm(e.getProject());
		form.getSemesterField().setText(PluginUtils.getSemesterID());

		form.show();
		if (form.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
			props.setValue(PluginUtils.KEY_SEMESTER, form.getSemesterField().getText());
		}
	}
}
