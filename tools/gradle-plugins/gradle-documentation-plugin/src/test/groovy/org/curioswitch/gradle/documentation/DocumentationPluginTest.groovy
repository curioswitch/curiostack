package org.curioswitch.gradle.documentation

import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class DocumentationPluginTest extends Specification {
  def 'should register task'() {
    given:
    def project = ProjectBuilder.builder().build()

    when:
    project.plugins.apply('org.curioswitch.gradle-documentation-plugin')

    then:
    Task task = project.tasks.findByName('buildDocumentation')
    task.group == 'documentation'

    and:
    project.extensions.findByName('documentation')
  }
}
