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

import Resolver from 'enhanced-resolve/lib/Resolver';

import { rewriteResolveRequest } from '../../resolve';

type ResolverRequest = import('enhanced-resolve/lib/common-types').ResolverRequest;

export default class FallbackResolverPlugin {
  public apply(resolver: Resolver) {
    const tapable = resolver as any;

    const target = tapable.ensureHook('resolve');

    tapable
      .ensureHook('described-resolve')
      .tapAsync(
        'FallbackResolverPlugin',
        (
          request: ResolverRequest,
          resolveContext: any,
          callback: () => void,
        ) => {
          const rewritten = rewriteResolveRequest(request.request);
          if (rewritten === request.request) {
            return callback();
          }

          // Cast to any since types don't match webpack definition
          return (resolver as any).doResolve(
            target,
            {
              ...request,
              request: rewritten,
            },
            `rewriting ${request.request} to ${rewritten}`,
            resolveContext,
            callback,
          );
        },
      );
  }
}
