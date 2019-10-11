/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import 'module-alias/register';
import { addAlias } from 'module-alias';

let RECURSIVE_RESOLVE = false;

addAlias('core-js', (_, request) => {
  if (RECURSIVE_RESOLVE) {
    return 'core-js';
  }
  RECURSIVE_RESOLVE = true;
  try {
    try {
      require.resolve(request);
      // No error, the module exists
      return 'core-js';
    } catch (e) {
      // Fall through to try core-js-2
    }
    try {
      require.resolve(request.replace('core-js/', 'core-js-2/'));
      // No error, the module exists in core-js-2
      return 'core-js-2';
    } catch (e) {
      // Not in core-js-2, just return the default (which will fail and provide an error message).
    }
  } finally {
    RECURSIVE_RESOLVE = false;
  }
  return 'core-js';
});
