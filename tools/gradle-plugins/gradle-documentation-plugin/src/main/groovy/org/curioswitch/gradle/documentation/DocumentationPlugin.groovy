package org.curioswitch.gradle.documentation

import org.curioswitch.gradle.documentation.extension.GradleDocumentationPluginExtension
import org.curioswitch.gradle.documentation.template.TemplateProcessor
import org.gradle.api.Project
import org.gradle.api.Plugin

class DocumentationPlugin implements Plugin<Project> {
  void apply(Project project) {
    def extension = project.extensions.create(
        'documentation', GradleDocumentationPluginExtension, project)

    project.tasks.register('buildDocumentation') {
      group = 'documentation'
      doLast {
        def templateFile = new File(extension.templateFilePath)
        if (!templateFile.isFile()) {
          throw new FileNotFoundException("Cannot find file $extension.templateFilePath")
        }

        def outputFile = new File(extension.templateOutputPath)
        outputFile.parentFile.mkdirs()
        outputFile.newWriter().withWriter { w ->
          w << new TemplateProcessor(templateFile).process()
        }
      }
    }
  }
}
