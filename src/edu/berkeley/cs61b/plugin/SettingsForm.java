package edu.berkeley.cs61b.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingsForm extends DialogWrapper {
	private JTextField semesterField;
	private JPanel content;

	SettingsForm(Project project) {
		super(project, false);
		setTitle("CS 61B Plugin Settings");
		init();
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return content;
	}

	@Nullable
	@Override
	protected ValidationInfo doValidate() {
		String semester = semesterField.getText();
		if (!semester.matches("^(sp|su|fa)[0-9]{2}$")) {
			return new ValidationInfo("Semester must be 'sp', 'su', or 'fa', followed by a two-digit year.", semesterField);
		}

		return null;
	}

	JTextField getSemesterField() {
		return semesterField;
	}
}
