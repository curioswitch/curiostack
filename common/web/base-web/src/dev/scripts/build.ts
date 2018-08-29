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
import { promisify } from 'util';

import rimraf from 'rimraf';
import { Configuration, Stats } from 'webpack';
import saneWebpack from 'webpack-sane-compiler';
import startReporting from 'webpack-sane-compiler-reporter';

import { appConfiguration, prerenderConfiguration } from '../webpack/prod';

import { check } from './check';

async function runWebpack(config: Configuration): Promise<{ stats: Stats}> {
  const compiler = saneWebpack(config);
  startReporting(compiler);
  return compiler.run();
}

async function run() {
  await promisify(rimraf)(path.resolve(process.cwd(), 'build'));

  await check();

  let result = await runWebpack(appConfiguration);
  if (result.stats.hasErrors()) {
    throw new Error();
  }
  if (prerenderConfiguration) {
    result = await runWebpack(prerenderConfiguration);
    if (result.stats.hasErrors()) {
      throw new Error();
    }
  }
}
if (require.main === module) {
  run()
    .then(() => process.exit(0))
    .catch((err) => {
      console.log('Error running webpack.', err);
      process.exit(1);
    });
}
