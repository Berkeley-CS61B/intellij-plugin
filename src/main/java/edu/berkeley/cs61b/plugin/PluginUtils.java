package edu.berkeley.cs61b.plugin;

import com.intellij.ide.util.PropertiesComponent;

import java.time.LocalDate;

public class PluginUtils {
	static final String PROPERTY_KEY_BASE = "cs61b_plugin.";
	static final String KEY_SEMESTER = PROPERTY_KEY_BASE + "semester";

	static String getSemesterID() {
		return PropertiesComponent.getInstance().getValue(KEY_SEMESTER, computeCurrentSemester());
	}

	static String computeCurrentSemester() {
		/*
		 Dec 15 to May 1: Spring
		 May 1 to July 15: Summer
		 July 15 to Dec 15: Fall
		 */
		LocalDate d = LocalDate.now();
		int year = d.getYear() % 100;
		int day = d.getDayOfYear();

		if (day < 121 /* May 1 */) {
			return "sp" + year;
		} else if (day < 196 /* July 15 */) {
			return "su" + year;
		} else if (day < 349 /* Dec 15 */) {
			return "fa" + year;
		} else {
			return "sp" + (year + 1);
		}
	}
}
