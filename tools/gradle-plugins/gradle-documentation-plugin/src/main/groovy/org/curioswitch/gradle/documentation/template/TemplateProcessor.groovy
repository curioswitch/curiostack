package org.curioswitch.gradle.documentation.template

import groovy.text.SimpleTemplateEngine

class TemplateProcessor {
  private File templateFile
  private File templateFileDir

  TemplateProcessor(String templateFilePath) {
    this(new File(templateFilePath))
  }

  TemplateProcessor(File templateFile) {
    if (!templateFile.isFile()) {
      throw new IllegalArgumentException('Provided template file does not exist.')
    }

    this.templateFile = templateFile
    this.templateFileDir = templateFile.parentFile
  }

  String process() {
    def binding = [include: this.taggedTextFileInclusion]
    def template = new SimpleTemplateEngine().createTemplate(templateFile).make(binding)
    return template.toString()
  }

  private Closure<TaggableText> taggedTextFileInclusion = { String path ->
    def file = new File(templateFileDir, path)
    return file.isFile() ? new TaggableText(file.text) : null
  }
}
