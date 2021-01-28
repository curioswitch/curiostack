package org.curioswitch.gradle.documentation;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DocumentationPluginTest {

  @Test
  void shouldRegisterExtensionAndTasks() {
    Project project = ProjectBuilder.builder().build();
    project.getPlugins().apply("org.curioswitch.gradle-documentation-plugin");

    Object extension = project.getExtensions().findByName("documentation");
    Assertions.assertNotNull(extension);

    Task task = project.getTasks().findByName("buildDocumentation");
    Assertions.assertNotNull(task);
    Assertions.assertEquals("documentation", task.getGroup());
  }

}
