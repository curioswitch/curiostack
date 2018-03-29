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

import React, { ReactElement } from 'react';
import { HelmetData } from 'react-helmet';

export interface Args {
  content: string;
  delayedScriptSrcs: string[];
  helmet: HelmetData;
  links: React.Component[];

  serializedStateJs: string;
  // tslint:disable-next-line:array-type
  styles: ReactElement<{}>[];
  extraStyles: JSX.Element[];
}

export default function({
  content,
  delayedScriptSrcs,
  helmet,
  links,
  serializedStateJs,
  styles,
  extraStyles,
}: Args) {
  return (
    <html {...helmet.htmlAttributes.toComponent()}>
      <head>
        {helmet.title.toComponent()}
        {helmet.meta.toComponent()}
        {links.map((link) => link)}
        {helmet.link.toComponent()}
        {helmet.style.toComponent()}
        {styles}
        {extraStyles.map((style) => style)}
        {helmet.script.toComponent()}
        <script
          dangerouslySetInnerHTML={{
            __html: `window.__PRELOADED_STATE__ = ${serializedStateJs};`,
          }}
        />
      </head>
      <body>
        <div id="app" dangerouslySetInnerHTML={{ __html: content }} />
        {delayedScriptSrcs.map((src) => <script key={src} src={src} />)}
      </body>
    </html>
  );
}
