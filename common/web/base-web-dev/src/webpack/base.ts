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

import fs from 'fs';
import path from 'path';

import { Configuration, DefinePlugin } from 'webpack';

export interface Webpack4Configuration extends Configuration {
  mode: 'development' | 'production';
}

const typescriptLoader = [
  'babel-loader',
  {
    loader: 'ts-loader',
    options: {
      compilerOptions: {
        noEmit: false,
      },
      transpileOnly: true,
      onlyCompileBundledFiles: true,
      reportFiles: ['src/**/*.{ts,tsx}'],
    },
  },
];

const entrypoint = ['src/app.js', 'src/app.ts']
  .map((relativePath) => path.join(process.cwd(), relativePath))
  .filter((p) => fs.existsSync(p))[0]!;

const configure = (options: any): Webpack4Configuration => ({
  mode: options.mode,
  entry: [entrypoint],
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
        use: {
          loader: 'babel-loader',
        },
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
        test: /\.(jpg|png|gif)$/,
        use: [
          'file-loader',
          {
            loader: 'image-webpack-loader',
            options: {
              mozjpeg: {
                quality: 65,
                progressive: true,
              },
              gifsicle: {
                optimizationLevel: 7,
                interlaced: false,
              },
              optipng: {
                enabled: false,
                optimizationLevel: 7,
                interlaced: false,
              },
              pngquant: {
                enabled: false,
                quality: '65-90',
                speed: 4,
              },
            },
          },
        ],
      },
      {
        test: /\.html$/,
        use: 'html-loader',
      },
      {
        test: /\.md$/,
        use: ['babel-loader', '@hugmanrique/react-markdown-loader'],
      },
    ],
  },
  plugins: [
    ...options.plugins,
    new DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify(process.env.NODE_ENV),
      },
    }),
  ],
  resolve: {
    modules: ['src', 'node_modules'],
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    mainFields: ['browser', 'module', 'jsnext:main', 'main'],
  },
  devtool: options.devtool,
  target: 'web',
});

export default configure;
