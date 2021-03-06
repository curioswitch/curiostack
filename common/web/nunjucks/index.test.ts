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

import * as nunjucks from 'nunjucks';
import Environment, { Template, Loader, FileSystemLoader, WebLoader } from '.';
import CurioNunjucksEnvironment from './src/CurioNunjucksEnvironment';

describe('Environment as the default export', () => {
  test('is a functional CurioNunjucksEnvironment', () => {
    const env = new Environment();

    expect(env).toBeInstanceOf(CurioNunjucksEnvironment);
    expect(
      env.renderString(
        "{{ 'foo\n_@@_\nbar\nbaz\n_@@_\nfoz\n_@@_\nbop\n_@@_\nbap' | allBetweenLines('@') }}",
        {},
      ),
    ).toMatch(/^bar\nbaz\nbop$/);
  });
});

describe('Non-default exports', () => {
  test('are correct nunjucks classes', () => {
    expect(Template).toBe(nunjucks.Template);
    expect(Loader).toBe(nunjucks.Loader);
    expect(FileSystemLoader).toBe(nunjucks.FileSystemLoader);
    expect(WebLoader).toBe(nunjucks.WebLoader);
  });
});
