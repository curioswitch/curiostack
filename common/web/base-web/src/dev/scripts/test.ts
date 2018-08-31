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

import { run } from 'jest-cli';

const CONFIG = {
  globals: {
    'ts-jest': {
      babelConfig: {
        presets: [
          [
            '@babel/env',
            {
              targets: {
                node: 'current',
              },
            },
          ],
          '@babel/react',
        ],
        plugins: [
          '@babel/plugin-transform-runtime',
          '@babel/proposal-class-properties',
          '@babel/proposal-async-generator-functions',
          '@babel/proposal-optional-catch-binding',
          '@babel/syntax-dynamic-import',
          '@babel/syntax-object-rest-spread',
          [
            'react-intl-auto',
            {
              removePrefix: 'src/',
            },
          ],
        ],
      },
    },
  },
  testURL: 'http://localhost',
  transform: {
    '^.+\\.tsx?$': 'ts-jest',
  },
  transformIgnorePatterns: ['node_modules/(?!@curiostack)'],
  testRegex: '(/__tests__/.*|(\\.|/)(test|spec))\\.(jsx?|tsx?)$',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  setupTestFrameworkScriptFile:
    '@curiostack/base-web/build/dev/testing/test-bundler.jsx',
  snapshotSerializers: ['enzyme-to-json/serializer'],
  moduleNameMapper: {
    '.*\\.(css|less|styl|scss|sass)$':
      '@curiostack/base-web/build/dev/testing/mocks/cssModule.js',
    '.*\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
      '@curiostack/base-web/build/dev/testing/mocks/image.js',
  },
};

export async function test() {
  const cli_params = process.argv.slice(2);
  const jest_params = ['--config', JSON.stringify(CONFIG)].concat(cli_params);
  return run(jest_params);
}

if (require.main === module) {
  test().catch(() => process.exit(1));
}
