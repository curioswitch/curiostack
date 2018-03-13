#!/usr/bin/env node
/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import proxy from 'koa-proxies';
import serve from 'webpack-serve';

import config from '../webpack/dev';

// tslint:disable-next-line:no-var-requires
const historyFallback = require('koa2-history-api-fallback');

// tslint:disable-next-line:no-var-requires
const pkg = require(path.resolve(process.cwd(), 'package.json'));

let add;
if (pkg.devServer && pkg.devServer.proxy) {
  add = (app: any, middleware: any) => {
    for (const urlPath of Object.keys(pkg.devServer.proxy)) {
      const target = pkg.devServer.proxy[urlPath];
      app.use(
        (proxy as any)(urlPath, {
          target,
          changeOrigin: true,
          secure: false,
        }),
      );
    }
    app.use(historyFallback());
    middleware.webpack();
    middleware.content();
  };
}

process.on('SIGINT', () => {
  console.log('sigint');
  process.exit();
});

serve({
  config,
  add,
  port: 3000,
}).catch((err) => {
  console.log('Error starting dev server.', err);
  process.exit(1);
});
