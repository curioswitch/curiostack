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

import '@babel/polyfill';

import {
  createMemoryHistory as createHistory,
  LocationDescriptor,
} from 'history';
import React from 'react';
import ReactDOMServer from 'react-dom/server';
import { Helmet } from 'react-helmet';
import { Provider } from 'react-redux';
import { StaticRouter } from 'react-router-dom';
import { Store } from 'redux';
import { ServerStyleSheet, ThemeProvider } from 'styled-components';

import { appConfig } from '../app';
import LanguageProvider, {
  LocaleMessages,
} from '../containers/LanguageProvider';
import initRedux from '../redux';

import Template from './template';

interface Props {
  messages: LocaleMessages;
  store: Store;
  location: LocationDescriptor;
  Component: React.ComponentClass<any> | React.StatelessComponent<any>;
  theme: any;
}

function RenderedPage({ messages, store, location, Component, theme }: Props) {
  return (
    <Provider store={store}>
      <LanguageProvider messages={messages}>
        <ThemeProvider theme={theme || {}}>
          <StaticRouter location={location} context={{}}>
            <Component />
          </StaticRouter>
        </ThemeProvider>
      </LanguageProvider>
    </Provider>
  );
}

export default function(locals: any) {
  const path = locals.path.replace('.html', '');
  // Create redux store with history
  const initialState = {
    ...appConfig.initialState,
    route: {
      location: {
        pathname: path,
      },
    },
  };
  const history = createHistory();
  const store = initRedux(initialState, history);

  const sheet = new ServerStyleSheet();
  const page = sheet.collectStyles(
    <RenderedPage
      messages={appConfig.messages}
      store={store}
      location={initialState.route.location}
      Component={appConfig.component}
      theme={appConfig.theme || {}}
    />,
  );
  const renderedContent = ReactDOMServer.renderToString(page);
  const helmet = Helmet.renderStatic();
  console.log(renderedContent);
  return ReactDOMServer.renderToStaticMarkup(
    <Template
      content={renderedContent}
      mainScriptSrc={locals.assets.main}
      helmet={helmet}
    />,
  );
}
