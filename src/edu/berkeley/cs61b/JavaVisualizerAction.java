package edu.berkeley.cs61b;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class JavaVisualizerAction extends AnAction {

	@Override
	public void actionPerformed(AnActionEvent e) {
		System.out.println("Java Visualizer");
	}
}
