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

import React from 'react';

import { LocaleMessages } from './containers/LanguageProvider';

export { default as injectReducer } from './hoc/injectReducer';
export { default as injectSaga } from './hoc/injectSaga';

export { default as LoadingIndicator } from './components/LoadingIndicator';

export { GlobalStateBase } from './state';
export * from './state/actions';
export * from './state/saga';

export interface WebappConfig {
  component: React.ComponentClass | React.StatelessComponent;
  messages: LocaleMessages;
  defaultLocale: string;
  initialState: any;
  mountNode?: string | HTMLElement;
  theme?: any;
}

export class PrerenderedPaths {
  [path: string]: object;
}

export interface PrerenderConfig {
  wrappingComponent?:
    | React.ComponentClass<{ children: JSX.Element }>
    | React.StatelessComponent<{ children: JSX.Element }>;
  paths: PrerenderedPaths;
  globals: object;
  extraStylesExtractor?: () => JSX.Element[];
}
