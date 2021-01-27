package org.curioswitch.gradle.documentation

import spock.lang.Specification
import org.gradle.testkit.runner.GradleRunner

class GradleDocumentationPluginFunctionalTest extends Specification {
  def 'should run task'() {
    given:
    def testResourceDirPath = '/home/vmeiren/git/gradle-documentation-plugin/src/test/resources'
    def testDir = new File('build/functionalTest')
    testDir.mkdirs()
    new File(testDir, 'settings.gradle') << ''
    new File(testDir, 'build.gradle') << """
      plugins {
        id('org.curioswitch.gradle-documentation-plugin')
      }
      
      documentation {
        templateFilePath = '$testResourceDirPath/template.md'
      }
    """

    and:
    def runner = GradleRunner.create()
        .forwardOutput()
        .withPluginClasspath()
        .withProjectDir(testDir)

    and:
    def expectedProcessedTemplate = new File(testResourceDirPath, 'template_processed.md').text

    when:
    runner.withArguments('buildDocumentation').build()

    then:
    new File(testDir, 'build/documentation/documentation.md').text == expectedProcessedTemplate

    cleanup:
    testDir.deleteDir()
  }
}
