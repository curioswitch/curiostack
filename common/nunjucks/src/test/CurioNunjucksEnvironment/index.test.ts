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

import CurioNunjucksEnvironment from '../../../src/main/CurioNunjucksEnvironment'

const nunjucks = new CurioNunjucksEnvironment()
const textWithTags = readFileSync(resolve(__dirname, '../resources/text_with_tags.txt'), 'utf8')

describe('filters', () => {
  test('exist', () => {
    expect(nunjucks.getFilter('allAfterTaggedLine')).toBeDefined()
    expect(nunjucks.getFilter('allBetweenTaggedLines')).toBeDefined()
  })

  test('working as intended', () => {
    expect(nunjucks.renderString(
      "{% filter allAfterTaggedLine('d1e') %}" + textWithTags + "{% endfilter %}"
    )).toMatch('\nParagraph after tags\n')
    expect(nunjucks.renderString(
      "{% filter allAfterTaggedLine('d1e', false) %}" + textWithTags + "{% endfilter %}"
    )).toMatch('\nParagraph after tags\n')
    expect(nunjucks.renderString(
      "{% filter allAfterTaggedLine('.*d1e.*') %}" + textWithTags + "{% endfilter %}"
    )).toMatch('')
    expect(nunjucks.renderString(
      "{% filter allAfterTaggedLine('.*d1e.*', true) %}" + textWithTags + "{% endfilter %}"
    )).toMatch('\nParagraph after tags\n')

    expect(nunjucks.renderString(
      "{% filter allBetweenTaggedLines('d1s', 'd1e') %}" + textWithTags + "{% endfilter %}"
    )).toMatch('Paragraph\nin tags\n')
    expect(nunjucks.renderString(
      "{% filter allBetweenTaggedLines('d1s', 'd1e', false) %}" + textWithTags + "{% endfilter %}"
    )).toMatch('Paragraph\nin tags\n')
    expect(nunjucks.renderString(
      "{% filter allBetweenTaggedLines('.*d1s.*', '.*d1e.*', false) %}" + textWithTags + "{% endfilter %}"
    )).toMatch('')
    expect(nunjucks.renderString(
      "{% filter allBetweenTaggedLines('.*d1s.*', '.*d1e.*', true) %}" + textWithTags + "{% endfilter %}"
    )).toMatch('Paragraph\nin tags\n')
  })
})
