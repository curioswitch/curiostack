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

import webpack from 'webpack';
import WebpackDevServer, { ProxyConfigMap } from 'webpack-dev-server';

import config from '../webpack/dev';

// tslint:disable-next-line:no-var-requires
const pkg = require(path.resolve(process.cwd(), 'package.json'));

let proxyConfig: ProxyConfigMap = {};
if (
  pkg.curiostack &&
  pkg.curiostack.devServer &&
  pkg.curiostack.devServer.proxy
) {
  const proxy = pkg.curiostack.devServer.proxy;
  proxyConfig = Object.keys(proxy).reduce(
    (merged: ProxyConfigMap, p) => ({
      [p]: {
        target: proxy[p],
        changeOrigin: true,
        secure: false,
      },
      ...merged,
    }),
    {},
  );
}

const options = {
  historyApiFallback: true,
  host: 'localhost',
  hot: true,
  noInfo: true,
  open: true,
  port: 3000,
  proxy: proxyConfig,
};

WebpackDevServer.addDevServerEntrypoints(config, options);

const server = new WebpackDevServer(webpack(config), options);

server.listen(3000, 'localhost', () => {
  console.log('dev server listening on port 3000');
});
