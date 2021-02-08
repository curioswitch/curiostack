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

import CurioNunjucksEnvironment from '.'

const nunjucksEnv = new CurioNunjucksEnvironment()
const textWithTags = readFileSync(resolve(__dirname, '../resources/text_with_tags.txt'), 'utf8')

describe('filters', () => {
  test('exist', () => {
    expect(nunjucksEnv.getFilter('allAfterLine')).toBeDefined()
    expect(nunjucksEnv.getFilter('allBetweenLines')).toBeDefined()
  })

  test('working as intended', () => {
    expect(nunjucksEnv.renderString(
      "{% filter allAfterLine('.*d1e.*') %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('')
    expect(nunjucksEnv.renderString(
      "{% filter allAfterLine('.*d1e.*', false) %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('\nParagraph after tags\n')
    expect(nunjucksEnv.renderString(
      "{% filter allAfterLine('d1e', true) %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('\nParagraph after tags\n')

    expect(nunjucksEnv.renderString(
      "{% filter allBetweenLines('.*(d1s|d1e).*') %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('Paragraph\nin tags\n')
    expect(nunjucksEnv.renderString(
      "{% filter allBetweenLines('.*(d1s|d1e).*', false) %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('')
    expect(nunjucksEnv.renderString(
      "{% filter allBetweenLines('d1', true) %}" + textWithTags + "{% endfilter %}", null
    )).toMatch('Paragraph\nin tags\n')
  })
})
