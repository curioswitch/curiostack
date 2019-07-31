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

/* eslint-disable import/first */

process.env.NODE_ENV = 'production';

require('ts-node').register({
  transpileOnly: true,
  ignore: [],
  compilerOptions: {
    module: 'commonjs',
    jsx: 'react',
    target: 'es2016',
  },
});

import fs from 'fs';
import path from 'path';

import { gzip } from '@gfx/zopfli';
import BrotliPlugin from 'brotli-webpack-plugin';
import CompressionPlugin from 'compression-webpack-plugin';
import CopyWebpackPlugin from 'copy-webpack-plugin';
import FaviconPlugin from 'favicons-webpack-plugin';
import HtmlWebpackPlugin from 'html-webpack-plugin';
import { ReactLoadablePlugin } from 'react-loadable/webpack';
import StaticSiteGeneratorPlugin from 'static-site-generator-webpack-plugin';
import { Configuration, DefinePlugin } from 'webpack';

import configureBase from './base';

const prerenderConfigPath = path.resolve(process.cwd(), 'src/prerender');
const prerenderConfig = ['.ts', '.tsx', '.js', '.jsx']
  .map((ext) => `${prerenderConfigPath}${ext}`)
  .some((p) => fs.existsSync(p))
  ? // eslint-disable-next-line import/no-dynamic-require
    require(prerenderConfigPath).default
  : undefined;

const loadableJsonPath = path.resolve(
  process.cwd(),
  'build/react-loadable.json',
);

const plugins = [
  new DefinePlugin({
    'process.env': {
      APP_CONFIG_PATH: JSON.stringify(path.resolve(process.cwd(), 'src/app')),
      NODE_ENV: JSON.stringify('production'),
    },
    WEBPACK_PRERENDERING: false,
  }),

  new ReactLoadablePlugin({
    filename: loadableJsonPath,
  }),

  new HtmlWebpackPlugin({
    inject: true,
    template: 'src/index.html',
    chunksSortMode: 'none',
  }),

  new FaviconPlugin({
    inject: false,
    logo: 'favicon.png',
    prefix: 'icons-[hash]/',
    emitStats: true,
    statsFilename: 'iconstats.json',
  }),

  new CopyWebpackPlugin([
    {
      from: 'app/shared/assets',
      to: 'assets',
      ignore: ['README.md'],
    },
  ]),

  new CompressionPlugin({
    filename: '[path].gz[query]',
    algorithm: (input: any, compressionOptions: any, callback: any) =>
      gzip(input, compressionOptions, callback),
    test: /\.(js|css|html|svg)$/,
    threshold: 1024,
    minRatio: 0.9,
  } as any),

  new BrotliPlugin({
    asset: '[path].br[query]',
    test: /\.(js|css|html|svg)$/,
    threshold: 1024,
    minRatio: 0.9,
  }),
];

export const appConfiguration: Configuration = configureBase({
  plugins,
  mode: 'production',
  babelPlugins: [
    '@babel/transform-react-constant-elements',
    '@babel/transform-react-inline-elements',
    'react-loadable/babel',
  ],
  output: {
    filename: '[name].[chunkhash].js',
    chunkFilename: '[name].[chunkhash].chunk.js',
    publicPath: '/static/',
  },
});

function createPrerenderConfiguration(): Configuration {
  const prerenderPlugins = [
    new DefinePlugin({
      'process.env': {
        APP_CONFIG_PATH: JSON.stringify(path.resolve(process.cwd(), 'src/app')),
        PRERENDER_CONFIG_PATH: JSON.stringify(
          path.resolve(process.cwd(), 'src/prerender'),
        ),
        NODE_ENV: JSON.stringify('production'),
        LOADABLE_JSON_PATH: JSON.stringify(loadableJsonPath),
        ICONSTATS_JSON_PATH: JSON.stringify(
          path.resolve(process.cwd(), 'build/web/iconstats.json'),
        ),
      },
      WEBPACK_PRERENDERING: true,
    }),

    new StaticSiteGeneratorPlugin({
      entry: 'prerender',
      paths: Object.keys(prerenderConfig.paths),
      locals: {
        pathStates: prerenderConfig.paths,
      },
      globals: {
        window: {},
        ...prerenderConfig.globals,
      },
    }),

    new CompressionPlugin({
      filename: '[path].gz[query]',
      algorithm: (input: any, compressionOptions: any, callback: any) =>
        gzip(input, compressionOptions, callback),
      test: /\.(html)$/,
      threshold: 1024,
      minRatio: 0.9,
    } as any),

    new BrotliPlugin({
      asset: '[path].br[query]',
      test: /\.(html)$/,
      threshold: 1024,
      minRatio: 0.9,
    }),
  ];

  return configureBase({
    plugins: prerenderPlugins,
    // This is only run during the build, so development mode is better to provide stacktraces.
    mode: 'development',
    target: 'node',
    entrypoints: {
      prerender: path.resolve(__dirname, '../../prerender/index.tsx'),
    },
    babelPlugins: [
      '@babel/transform-react-constant-elements',
      '@babel/transform-react-inline-elements',
      'react-loadable/babel',
      'dynamic-import-node',
    ],
    babelTargets: {
      node: 'current',
    },
    output: {
      filename: '../prerender/[name].js',
      chunkFilename: '../prerender/[name].chunk.js',
      publicPath: '/static/',
      libraryTarget: 'commonjs',
    },
  });
}

export const prerenderConfiguration = prerenderConfig
  ? createPrerenderConfiguration()
  : undefined;
