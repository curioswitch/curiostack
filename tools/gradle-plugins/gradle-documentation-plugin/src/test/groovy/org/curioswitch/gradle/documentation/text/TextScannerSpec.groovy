package org.curioswitch.gradle.documentation.text

import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class TextScannerSpec extends Specification {

  def 'Should parse text correctly'() {
    given: 'a TextScanner initialized with some textWithTags'
    String textWithTags = getClass().getResource("/docs/text_with_tags.md").text
    def scanner = new TextScanner(textWithTags)

    when: 'performing a particular scanMethodCall'
    String output = scanMethodCall(scanner)

    then: 'the expectedOutput is received'
    output == expectedOutput

    where:
    scanMethodCall                                                                                     || expectedOutput
    { TextScanner s -> s.getAllAfterLineContaining('d1s') }                                            || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllAfterLineContaining('d1e') }                                            || '\nPragraph after tags\n'
    { TextScanner s -> s.getAllAfterLineContaining('wrong_tag') }                                      || ''
    { TextScanner s -> s.getAllBetweenLinesContaining('d1s', 'd1e') }                                  || 'Paragrpah\nin tags\n'
    { TextScanner s -> s.getAllBetweenLinesContaining('d1s', 'wrong_tag') }                            || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllBetweenLinesContaining('d1e', 'wrong_tag') }                            || '\nPragraph after tags\n'
    { TextScanner s -> s.getAllBetweenLinesContaining('wrong_tag', 'd1e') }                            || ''
    { TextScanner s -> s.getAllAfterLineMatching(/.*d1s.*/) }                                          || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllAfterLineMatching(/.*d1e.*/) }                                          || '\nPragraph after tags\n'
    { TextScanner s -> s.getAllAfterLineMatching(/d1s/) }                                              || ''
    { TextScanner s -> s.getAllBetweenLinesMatching(/.*d1s.*/, /.*d1e.*/) }                            || 'Paragrpah\nin tags\n'
    { TextScanner s -> s.getAllBetweenLinesMatching(/.*d1s.*/, /.*wrong_tag.*/) }                      || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllBetweenLinesMatching(/.*d1s.*/, /d1e/) }                                || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllBetweenLinesMatching(/.*d1e.*/, /wrong_tag/) }                          || '\nPragraph after tags\n'
    { TextScanner s -> s.getAllAfterLineAsserting { it.contains('d1s') } }                             || 'Paragrpah\nin tags\n<!--- d1e -->\n\nPragraph after tags\n'
    { TextScanner s -> s.getAllBetweenLinesAsserting({ it.contains('d1s') }, { it.contains('d1e') }) } || 'Paragrpah\nin tags\n'
  }

  def 'Should load text from file'() {
    given: 'a file with some content'
    def file = new File(getClass().getResource("/docs/text_with_tags.md").path)

    when: 'initializing a TextScanner with the file'
    def textScanner = new TextScanner(file)

    then: 'the TextScanner has the file content loaded into memory'
    textScanner.text == file.text
  }

}
