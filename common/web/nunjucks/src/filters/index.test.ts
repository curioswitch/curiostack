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

import { readFileSync } from 'fs';
import { resolve } from 'path';

import { allAfterLine, allBetweenLines } from '.';

const textWithTags = readFileSync(
  resolve(__dirname, '../resources/text_with_tags.txt'),
  'utf8',
);
const textWithMultipleTaggedBlocks = readFileSync(
  resolve(__dirname, '../resources/text_with_multiple_tagged_blocks.txt'),
  'utf8',
);

const textInTags = 'Paragraph\nin tags';
const textAfterFirstTag =
  'Paragraph\nin tags\n<!--- d1e -->\n\nParagraph after tags\n';

describe('allAfterLine', () => {
  test('works with partial match', () => {
    expect(allAfterLine(textWithTags, 'd1s')).toBe(textAfterFirstTag);
    expect(allAfterLine(textWithTags, 'd1s', false)).toBe(textAfterFirstTag);
  });

  test('works with exact match', () => {
    expect(allAfterLine(textWithTags, '.*d1s.*', true)).toBe(textAfterFirstTag);
  });

  test('works with wrong match', () => {
    expect(allAfterLine(textWithTags, 'wrong')).toBe('');
    expect(allAfterLine(textWithTags, '.*(d1s|d1e)', true)).toBe('');
  });
});

describe('allBetweenLines', () => {
  test('works with partial match', () => {
    expect(allBetweenLines(textWithTags, '(d1s|d1e)')).toBe(textInTags);
    expect(allBetweenLines(textWithTags, '(d1s|d1e)', false)).toBe(textInTags);
  });

  test('works with exact match', () => {
    expect(allBetweenLines(textWithTags, '.*(d1s|d1e).*', true)).toBe(
      textInTags,
    );
  });

  test('works with multiple blocks', () => {
    expect(allBetweenLines(textWithMultipleTaggedBlocks, 'd1s')).toBe(
      'Paragraph\nin tags\nOther tagged\nparagraph',
    );
  });

  test('works with wrong match', () => {
    expect(allBetweenLines(textWithTags, 'd1s')).toBe(textAfterFirstTag);
    expect(allBetweenLines(textWithTags, 'wrong')).toBe('');
    expect(allBetweenLines(textWithTags, '.*d1s.*', true)).toBe(
      textAfterFirstTag,
    );
    expect(allBetweenLines(textWithTags, '.*(d1s|d1e)', true)).toBe('');
  });
});
