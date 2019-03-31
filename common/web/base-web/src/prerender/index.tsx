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

// tslint:disable:jsx-no-lambda

import {
  createLocation,
  createMemoryHistory as createHistory,
  LocationDescriptor,
} from 'history';
import { Parser } from 'html-to-react';
import React from 'react';
import ReactDOMServer from 'react-dom/server';
import { Helmet } from 'react-helmet';
import Loadable from 'react-loadable';
import { Provider } from 'react-redux';
import { StaticRouter } from 'react-router-dom';
import { Store } from 'redux';
import serialize from 'serialize-javascript';
import { ServerStyleSheet, ThemeProvider } from 'styled-components';

import { appConfig } from '../app';
import LanguageProvider, {
  LocaleMessages,
} from '../containers/LanguageProvider';
import { PrerenderConfig } from '../index';
import initRedux from '../state/init';
import { routeInitialState } from '../state/reducers';

import Template from './template';

// eslint-disable-next-line @typescript-eslint/no-var-requires,import/no-dynamic-require
const LOADED_MODULES = require(process.env.LOADABLE_JSON_PATH!);
// eslint-disable-next-line @typescript-eslint/no-var-requires,import/no-dynamic-require
const ICON_STATS = require(process.env.ICONSTATS_JSON_PATH!);
// eslint-disable-next-line @typescript-eslint/no-var-requires,import/no-dynamic-require
const PRERENDER_CONFIG: PrerenderConfig = require(process.env
  .PRERENDER_CONFIG_PATH!).default;

interface Props {
  messages: LocaleMessages;
  store: Store;
  location: LocationDescriptor;
  component: JSX.Element;
  theme: any;
  modules: string[];
}

function RenderedPage({
  messages,
  store,
  location,
  modules,
  component,
  theme,
}: Props) {
  return (
    <Provider store={store}>
      <LanguageProvider messages={messages}>
        <ThemeProvider theme={theme || {}}>
          <StaticRouter location={location} context={{}}>
            <Loadable.Capture report={(name) => modules.push(name)}>
              {component}
            </Loadable.Capture>
          </StaticRouter>
        </ThemeProvider>
      </LanguageProvider>
    </Provider>
  );
}

function getBundles(manifest: any, moduleIds: string[]): any {
  return moduleIds.reduce(
    (bundles, moduleId) => bundles.concat(manifest[moduleId]),
    [],
  );
}

async function run(locals: any) {
  await Loadable.preloadAll();

  const p = locals.path.replace('.html', '');
  // Create redux store with history
  const initialState = {
    ...appConfig.initialState,
    router: routeInitialState.set('location', createLocation(p)),
    ...locals.pathStates[locals.path],
  };
  const history = createHistory();
  const store = initRedux(initialState, history);

  const sheet = new ServerStyleSheet();
  const modules: string[] = [];
  const WrappingComponent = PRERENDER_CONFIG.wrappingComponent;
  const component = WrappingComponent ? (
    <WrappingComponent>
      <appConfig.component />
    </WrappingComponent>
  ) : (
    <appConfig.component />
  );
  const page = sheet.collectStyles(
    <RenderedPage
      messages={appConfig.messages}
      store={store}
      location={initialState.router.location}
      modules={modules}
      component={component}
      theme={appConfig.theme || {}}
    />,
  );

  const renderedContent = ReactDOMServer.renderToString(page);
  const helmet = Helmet.renderStatic();

  const prerenderedBundleSources = getBundles(LOADED_MODULES, modules).map(
    (bundle: any) => `/static/${bundle.file}`,
  );

  // There is only one entry called main, so if we find a match we know it's filename.
  let mainScriptFilename;
  const chunkLists: any[][] = Object.values(LOADED_MODULES);
  for (const chunkList of chunkLists) {
    for (const chunk of chunkList) {
      if (chunk.file.startsWith('main.') && chunk.file.endsWith('.js')) {
        mainScriptFilename = chunk.file;
        break;
      }
    }
    if (mainScriptFilename) {
      break;
    }
  }
  const scripts = [
    ...prerenderedBundleSources,
    `/static/${mainScriptFilename!}`,
  ];

  const htmlParser = new Parser();
  const iconLinks = ICON_STATS.html.map((link: string) =>
    htmlParser.parse(link),
  );

  return ReactDOMServer.renderToStaticMarkup(
    <Template
      content={renderedContent}
      delayedScriptSrcs={scripts}
      helmet={helmet}
      links={iconLinks}
      serializedStateJs={serialize(initialState)}
      styles={sheet.getStyleElement()}
      extraStyles={
        PRERENDER_CONFIG.extraStylesExtractor
          ? PRERENDER_CONFIG.extraStylesExtractor()
          : []
      }
    />,
  );
}

export default function(locals: any) {
  return run(locals);
}
