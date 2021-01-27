package org.curioswitch.gradle.documentation.extension

import org.gradle.api.Project

class GradleDocumentationPluginExtension {
  private final Project project
  private static final String DEFAULT_DOC_NAME = 'documentation.md'

  String templateFilePath
  String templateOutputPath

  GradleDocumentationPluginExtension(Project project) {
    this.project = project
  }

  String getTemplateFilePath() {
    return templateFilePath ?: "$project.projectDir/$DEFAULT_DOC_NAME"
  }

  String getTemplateOutputPath() {
    return templateOutputPath ?: "$project.buildDir/documentation/$DEFAULT_DOC_NAME"
  }
}
