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

import { readFileSync } from 'fs'
import { resolve } from 'path'

import { allAfterTaggedLine, allBetweenTaggedLines } from '../../../src/main/filters'

const textWithTags = readFileSync(resolve(__dirname, '../resources/text_with_tags.txt'), 'utf8')

describe('allAfterTaggedLine', () => {
  const expectedResult = 'Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n'

  test('works with string tag', () => {
    expect(allAfterTaggedLine(textWithTags, 'd1s')).toMatch(expectedResult)
    expect(allAfterTaggedLine(textWithTags, 'd1s', false)).toMatch(expectedResult)
  })

  test('works with RegExp tag', () => {
    expect(allAfterTaggedLine(textWithTags, '.*d1s.*', true)).toMatch(expectedResult)
  })

  test('works with wrong string tag', () => {
    expect(allAfterTaggedLine(textWithTags, 'wrong')).toMatch('')
  })

  test('works with wrong RegExp tag', () => {
    expect(allAfterTaggedLine(textWithTags, 'd1s', true)).toMatch('')
    expect(allAfterTaggedLine(textWithTags, '.*wrong.*', true)).toMatch('')
  })
})

describe('allBetweenTaggedLines', () => {
  test('works with string tags', () => {
    expect(allBetweenTaggedLines(textWithTags, 'd1s', 'd1e')).toMatch('Paragraph\nin tags\n')
    expect(allBetweenTaggedLines(textWithTags, 'd1s', 'd1e', false)).toMatch('Paragraph\nin tags\n')
  })

  test('works with RegExp tags', () => {
    expect(allBetweenTaggedLines(textWithTags, '.*d1s.*', '.*d1e.*', true))
      .toMatch('Paragraph\nin tags\n')
  })

  test('works with wrong string tags', () => {
    expect(allBetweenTaggedLines(textWithTags, 'd1s', 'wrong'))
      .toMatch('Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n')
    expect(allBetweenTaggedLines(textWithTags, 'd1e', 'd1e')).toMatch('\nParagraph after tags\n')
    expect(allBetweenTaggedLines(textWithTags, 'wrong', 'd1e')).toMatch('')
  })

  test('works with wrong RegExp tags', () => {
    expect(allBetweenTaggedLines(textWithTags, '.*d1s.*', 'wrong', true))
      .toMatch('Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n')
    expect(allBetweenTaggedLines(textWithTags, '.*d1e.*', '.*d1e.*', true))
      .toMatch('\nParagraph after tags\n')
    expect(allBetweenTaggedLines(textWithTags, 'wrong', '.*d1e.*', true)).toMatch('')
  })
})
