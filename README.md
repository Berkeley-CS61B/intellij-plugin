# CS 61B Plugin
![Build](https://github.com/Berkeley-CS61B/intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/edu.berkeley.cs61b.plugin.svg)](https://plugins.jetbrains.com/plugin/edu.berkeley.cs61b.plugin)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/edu.berkeley.cs61b.plugin.svg)](https://plugins.jetbrains.com/plugin/edu.berkeley.cs61b.plugin)

<!-- Plugin description -->
Plugin for CS 61B at UC Berkeley. Includes a style checker (Checkstyle). This plugin no longer includes the Java Visualizer: it has been split off into a separate plugin: https://plugins.jetbrains.com/plugin/11512-java-visualizer

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CS 61B"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/Berkeley-CS61B/intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Updating style checker
1. Add the cs61b_<semester>_checks.xml file in the [style_config](./src/main/resources/style_config) folder.
2. For suppressions, add the cs61b_<semester>_suppressions.xml file in the [style_config](./src/main/resources/style_config) folder.
3. Add the semester's config line in [index.txt](./src/main/resources/style_config/index.txt). The line should have the format
  ```(regex for current semester)<tab character>(checks xml)<tab character>(suppressions xml)```
4. Update the plugin version in [gradle.properties](./gradle.properties)
5. Update the Change Notes under "[Unreleased]"


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
