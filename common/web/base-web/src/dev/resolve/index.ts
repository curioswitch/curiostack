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

import path from 'path';

import { DISABLER } from '../scripts/common';

// eslint-disable-next-line
const packageJson = require(path.resolve(process.cwd(), 'package.json'));

interface ResolveOverrides {
  [key: string]: string[];
}

const packageOverrides = packageJson.curiostack
  ? packageJson.curiostack.resolveOverrides
  : {};

export const config: ResolveOverrides = {
  'core-js': ['core-js', 'core-js-2'],
  ...packageOverrides,
};

export function rewriteResolveRequest(request: string): string {
  for (const [lib, overrides] of Object.entries(config)) {
    if (request !== lib && !request.startsWith(`${lib}/`)) {
      continue;
    }

    const remainingRequest = request.substring(request.indexOf('/') + 1);
    for (const override of overrides) {
      const rewritten = `${override}/${remainingRequest}`;
      DISABLER.DISABLE_ALIAS = true;
      try {
        require.resolve(rewritten);
        // Resolved so we found a working override.
        return rewritten;
      } catch (e) {
        // Fall through to try other overrides.
      } finally {
        DISABLER.DISABLE_ALIAS = false;
      }
    }
  }
  return request;
}
