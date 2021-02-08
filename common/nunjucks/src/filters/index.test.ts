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

import { allAfterLine, allBetweenLines } from '.'

const textWithTags = readFileSync(resolve(__dirname, '../resources/text_with_tags.txt'), 'utf8')

describe('allAfterLine', () => {
  const expectedResult = 'Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n'

  test('works with regex matcher', () => {
    expect(allAfterLine(textWithTags, '.*d1s.*')).toMatch(expectedResult)
    expect(allAfterLine(textWithTags, '.*d1s.*', false)).toMatch(expectedResult)
  })

  test('works with string inclusion matcher', () => {
    expect(allAfterLine(textWithTags, 'd1s', true)).toMatch(expectedResult)
  })

  test('works with wrong regex matcher', () => {
    expect(allAfterLine(textWithTags, 'd1s')).toMatch('')
    expect(allAfterLine(textWithTags, '.*wrong.*')).toMatch('')
  })

  test('works with wrong string inclusion matcher', () => {
    expect(allAfterLine(textWithTags, 'wrong', true)).toMatch('')
  })
})

describe('allBetweenLines', () => {
  test('works with regex matcher', () => {
    expect(allBetweenLines(textWithTags, '.*(d1s|d1e).*')).toMatch('Paragraph\nin tags\n')
    expect(allBetweenLines(textWithTags, '.*(d1s|d1e).*', false)).toMatch('Paragraph\nin tags\n')
  })

  test('works with string inclusion matcher', () => {
    expect(allBetweenLines(textWithTags, 'd1', true)).toMatch('Paragraph\nin tags\n')
  })

  test('works with wrong regex matcher', () => {
    expect(allBetweenLines(textWithTags, '.*d1s.*'))
      .toMatch('Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n')
    expect(allBetweenLines(textWithTags, 'wrong')).toMatch('')
  })

  test('works with wrong string inclusion matcher', () => {
    expect(allBetweenLines(textWithTags, 'd1s', true))
      .toMatch('Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n')
    expect(allBetweenLines(textWithTags, 'wrong', true)).toMatch('')
  })
})
