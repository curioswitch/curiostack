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

import CopyWebpackPlugin from 'copy-webpack-plugin';
import TsconfigPathsPlugin from 'tsconfig-paths-webpack-plugin';
import { Configuration } from 'webpack';

import FallbackResolverPlugin from './fallback-resolver';

// eslint-disable-next-line
const packageJson = require(path.resolve(process.cwd(), 'package.json'));

const browsers: string[] = (packageJson.curiostack &&
  // tslint:disable-next-line:strict-boolean-expressions
  packageJson.curiostack.browsers) || ['>1%', 'not ie 11', 'not op_mini all'];

if (browsers.includes('last 2 versions')) {
  // eslint-disable-next-line no-console
  console.warn(
    `${'Your value for curiostack.browsers contains "last 2 versions". This should be avoided as ' +
      'it includes a whole range of ancient browsers, including IE10. Please check out ' +
      'https://github.com/browserslist/browserslist for better possible values and when in ' +
      'doubt, "defaults" is still fine. Current value: '}${browsers}`,
  );
}

function configure(options: any): Configuration {
  const typescriptLoader = [
    {
      loader: 'thread-loader',
    },
    {
      loader: 'babel-loader',
      options: {
        presets: [
          [
            '@babel/env',
            {
              modules: false,
              useBuiltIns: 'usage',
              corejs: '3.2',
              targets: options.babelTargets || {
                browsers,
              },
            },
          ],
          '@babel/react',
        ],
        plugins: [
          [
            '@babel/plugin-transform-runtime',
            {
              corejs: 3,
              useESModules: true,
            },
          ],
          '@babel/proposal-class-properties',
          '@babel/proposal-async-generator-functions',
          '@babel/proposal-optional-catch-binding',
          '@babel/syntax-dynamic-import',
          '@babel/syntax-object-rest-spread',
          'react-hot-loader/babel',
          [
            'react-intl-auto',
            {
              removePrefix: 'src/',
            },
          ],
          [
            'babel-plugin-styled-components',
            {
              ssr: true,
            },
          ],
          ...options.babelPlugins,
        ],
      },
    },
    {
      loader: 'ts-loader',
      options: {
        compilerOptions: {
          noEmit: false,
        },
        allowTsInNodeModules: true,
        happyPackMode: true,
        experimentalFileCaching: true,
        experimentalWatchApi: true,
        onlyCompileBundledFiles: true,
        reportFiles: ['src/**/*.{ts,tsx}'],
      },
    },
  ];
  return {
    mode: options.mode,
    entry: options.entrypoints || {
      main: [path.resolve(__dirname, '../../app/entrypoint')],
    },
    output: {
      path: path.resolve(process.cwd(), 'build/web'),
      publicPath: '/',
      ...options.output,
    },
    module: {
      rules: [
        {
          test: /\.js$/, // Transform all .js files required somewhere with Babel
          exclude: /node_modules/,
          use: [
            {
              loader: 'thread-loader',
            },
            {
              loader: 'babel-loader',
            },
          ],
        },
        {
          test: /\.ts(x?)$/,
          exclude: /node_modules/,
          use: typescriptLoader,
        },
        {
          test: /\.ts(x?)$/,
          include: /node_modules\/@curiostack\/base-web/,
          use: typescriptLoader,
        },
        {
          test: /\.css$/,
          include: /node_modules/,
          use: 'raw-loader',
        },
        {
          test: /\.(eot|svg|otf|ttf|woff|woff2|webm|m4a|mp4)$/,
          use: 'file-loader',
        },
        {
          test: /\.(jpg|png)$/,
          use: [
            {
              loader: path.resolve(__dirname, './curio-image-loader'),
              options: {},
            },
          ],
        },
        {
          test: /\.html$/,
          use: 'html-loader',
        },
        {
          test: /\.md$/,
          use: [
            'thread-loader',
            'babel-loader',
            '@hugmanrique/react-markdown-loader',
          ],
        },
      ],
    },
    plugins: [
      new CopyWebpackPlugin([
        {
          from: 'app/shared/assets',
          to: 'assets',
          ignore: ['README.md'],
        },
      ]),
      ...options.plugins,
    ],
    resolve: {
      modules: ['src', 'node_modules'],
      extensions: ['.js', '.jsx', '.ts', '.tsx'],
      mainFields: ['browser', 'module', 'jsnext:main', 'main'],
      plugins: [
        new FallbackResolverPlugin(),
        new TsconfigPathsPlugin({
          extensions: ['.ts', '.tsx', '.js', '.jsx'],
          mainFields: ['browser', 'module', 'jsnext:main', 'main'],
        }),
      ],
    },
    devtool: options.devtool,
    target: options.target || 'web',
    ...options.optimizataion,
  };
}

export default configure;
