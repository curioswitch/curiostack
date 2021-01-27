package org.curioswitch.gradle.documentation;

import org.gradle.api.Project;

import java.io.File;
import java.util.Optional;

public class DocumentationExtension {
  private final Project project;
  private static final String DEFAULT_DOC_NAME = "documentation.md";

  public DocumentationExtension(Project project) {
    this.project = project;
  }


  private String templateFilePath;
  private String templateOutputPath;

  public void setTemplateFilePath(String templateFilePath) {
    this.templateFilePath = templateFilePath;
  }

  public void setTemplateOutputPath(String templateOutputPath) {
    this.templateOutputPath = templateOutputPath;
  }

  String getTemplateFilePath() {
    return Optional.ofNullable(templateFilePath).orElse(
        new File(project.getProjectDir(), DEFAULT_DOC_NAME).getAbsolutePath());
  }

  String getTemplateOutputPath() {
    return Optional.ofNullable(templateOutputPath).orElse(
        new File(project.getBuildDir(), "documentation/" + DEFAULT_DOC_NAME).getAbsolutePath());
  }
}
