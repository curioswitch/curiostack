/*
 * MIT License
 *
 * Copyright (c) 2021 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.curioswitch.gradle.documentation;

import com.google.common.io.Files;
import org.curioswitch.gradle.documentation.template.TemplateProcessor;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class DocumentationPlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    DocumentationExtension extension = project.getExtensions().create(
        "documentation", DocumentationExtension.class, project);

    project.getTasks().register("buildDocumentation", (Task task) -> {
      task.setGroup("documentation");
      task.doLast((Task ignored) -> {
        String templateFilePath = extension.getTemplateFilePath();
        var templateFile = new File(templateFilePath);
        if (!templateFile.isFile()) {
          throw new RuntimeException(
              new FileNotFoundException("Cannot find file " + templateFilePath));
        }

        var outputFile = new File(extension.getTemplateOutputPath());
        outputFile.getParentFile().mkdirs();
        String content = new TemplateProcessor(templateFile).process();
        try {
          Files.write(content.getBytes(StandardCharsets.UTF_8), outputFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    });
  }
}
