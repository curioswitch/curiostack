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

// tslint:disable:max-classes-per-file

// TODO(choko): Figure out why @types/intl isn't enough.
declare module 'intl' {

}

declare module 'intl/locale-data/jsonp/*.js' {

}

declare module '*.json' {
  const value: {
    version: string;
  };
  export = value;
}

declare module 'autodll-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Entry {
    [key: string]: string[];
  }
  interface Args {
    entry?: Entry;
    filename?: string;
    context?: string;
    inject?: boolean;
    path?: string;
    debug?: boolean;
    plugins?: Plugin[];
    inherit?: boolean;
  }
  class AutoDllPlugin extends Plugin {
    constructor(args: Args);
  }
  export = AutoDllPlugin;
}

declare module 'brotli-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Args {
    asset: string;
    test: RegExp;
    threshold: number;
    minRatio: number;
  }
  class BrotliPlugin extends Plugin {
    constructor(args: Args);
  }
  export = BrotliPlugin;
}

declare module 'fork-ts-checker-webpack-plugin' {
  import { Plugin } from 'webpack';
  class ForkTsCheckerWebpackPlugin extends Plugin {}
  export = ForkTsCheckerWebpackPlugin;
}

declare module 'html-to-react' {
  import { Component } from 'react';

  class Parser {
    public parse(html: string): Component;
  }
}

declare module 'koa-proxies' {

}

declare module 'react-loadable/webpack' {
  import { Plugin } from 'webpack';
  interface Args {
    filename: string;
  }
  export class ReactLoadablePlugin extends Plugin {
    constructor(args: Args);
  }
}

declare module 'static-site-generator-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Args {
    entry: string;
    paths: string[];
    locals: any;
    globals: any;
  }
  class StaticSiteGeneratorPlugin extends Plugin {
    constructor(args: Args);
  }
  export = StaticSiteGeneratorPlugin;
}

declare module 'favicons-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Args {
    logo: string;
    inject?: boolean;
    prefix?: string;
    emitStats?: boolean;
    statsFilename?: string;
    persistentCache?: boolean;
  }
  class FaviconsPlugin extends Plugin {
    constructor(args: Args);
  }
  export = FaviconsPlugin;
}

declare module 'webpack-sane-compiler' {
  import { Configuration, Stats } from 'webpack';
  class SaneCompiler {
    public run(): Promise<{ stats: Stats }>;
  }
  function saneWebpack(config: Configuration): SaneCompiler;
  export = saneWebpack;
}

declare module 'webpack-sane-compiler-reporter' {
  import { SaneCompiler } from 'webpack-sane-compiler';

  function startReporting(compiler: SaneCompiler): any;
  export = startReporting;
}

declare module 'webpack-serve' {
  import { Webpack4Configuration } from './webpack/base';
  export interface Args {
    config: Webpack4Configuration;
    port: number;
    add?: (any, any, any) => void;
  }
  export default function serve(args: Args): Promise<{}>;
}

declare module 'zopfli-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Args {
    asset: string;
    algorithm: string;
    test: RegExp;
    threshold: number;
    minRatio: number;
  }
  class ZopfliPlugin extends Plugin {
    constructor(args: Args);
  }
  export = ZopfliPlugin;
}
