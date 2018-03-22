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

process.env.NODE_ENV = 'production';

import path from 'path';

import BrotliPlugin from 'brotli-webpack-plugin';
import StaticSiteGeneratorPlugin from 'static-site-generator-webpack-plugin';
import WebappPlugin from 'webapp-webpack-plugin';
import { Configuration, DefinePlugin } from 'webpack';
import ZopfliPlugin from 'zopfli-webpack-plugin';

import configureBase from './base';

const plugins = [
  new DefinePlugin({
    'process.env': {
      APP_CONFIG_PATH: JSON.stringify(path.resolve(process.cwd(), 'src/app')),
      NODE_ENV: JSON.stringify('production'),
    },
  }),

  new StaticSiteGeneratorPlugin({
    entry: 'prerender',
    paths: ['/'],
    globals: {
      window: {},
    },
  }),

  new WebappPlugin({
    logo: 'favicon.png',
    prefix: 'icons-[hash]/',
    emitStats: true,
    statsFilename: 'iconstats.json',
  }),

  new ZopfliPlugin({
    asset: '[path].gz[query]',
    algorithm: 'zopfli',
    test: /\.(js|css|html|svg)$/,
    threshold: 1024,
    minRatio: 0.9,
  }),

  new BrotliPlugin({
    asset: '[path].br[query]',
    test: /\.(js|css|html|svg)$/,
    threshold: 1024,
    minRatio: 0.9,
  }),
];

const configuration: Configuration = configureBase({
  plugins,
  mode: 'production',
  additionalEntrypoints: {
    prerender: path.resolve(__dirname, '../../prerender/index.tsx'),
  },
  babelPlugins: [
    '@babel/transform-react-constant-elements',
    '@babel/transform-react-inline-elements',
  ],
  output: {
    filename: '[name].[chunkhash].js',
    chunkFilename: '[name].[chunkhash].chunk.js',
    publicPath: '/static/',
  },
});
export default configuration;
