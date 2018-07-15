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

declare module 'koa-proxies' {

}

declare module 'webapp-webpack-plugin' {
  import { Plugin } from 'webpack';
  interface Args {
    logo: string;
    prefix?: string;
    emitStats?: boolean;
    statsFilename?: string;
  }
  class WebappPlugin extends Plugin {
    constructor(args: Args);
  }
  export = WebappPlugin;
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
