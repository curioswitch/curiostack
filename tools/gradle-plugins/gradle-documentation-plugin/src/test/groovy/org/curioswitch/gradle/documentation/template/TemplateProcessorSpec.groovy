package org.curioswitch.gradle.documentation.template

import spock.lang.Specification
import spock.lang.Subject

class TemplateProcessorSpec extends Specification {

  def 'should process the template'() {
    given:
    def templateProcessor = new TemplateProcessor(
        getClass().getResource("/template.md").getPath())

    when:
    def processedTemplate = templateProcessor.process()

    then:
    processedTemplate == getClass().getResource("/template_processed.md").text
  }

}
