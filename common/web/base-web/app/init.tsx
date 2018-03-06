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

import 'babel-polyfill';

import { createBrowserHistory, History } from 'history';
import React from 'react';
import ReactDOM from 'react-dom';
import { Provider } from 'react-redux';
import { ConnectedRouter } from 'react-router-redux';
import { Store } from 'redux';
import { ThemeProvider } from 'styled-components';

import LanguageProvider, {
  LocaleMessages,
} from '../containers/LanguageProvider';
import initI18n from '../i18n/init';
import { WebappConfig } from '../index';
import initRedux from '../redux/init';

function render(
  messages: LocaleMessages,
  store: Store,
  history: History,
  mountNode: HTMLElement,
  Component: React.ComponentClass | React.StatelessComponent,
  theme: any,
) {
  ReactDOM.render(
    <Provider store={store}>
      <LanguageProvider messages={messages}>
        <ThemeProvider theme={theme || {}}>
          <ConnectedRouter history={history}>
            <Component />
          </ConnectedRouter>
        </ThemeProvider>
      </LanguageProvider>
    </Provider>,
    mountNode,
  );
}

export default function init(config: WebappConfig) {
  const history = createBrowserHistory();
  const store = initRedux(config.initialState, history);
  const mountNode = config.mountNode
    ? typeof config.mountNode === 'string'
      ? document.getElementById(config.mountNode)!
      : config.mountNode
    : document.getElementById('app')!;
  const formattedMessages = initI18n(config.defaultLocale, config.messages);

  const doRender = (
    component: React.ComponentClass | React.StatelessComponent,
  ) =>
    render(
      formattedMessages,
      store,
      history,
      mountNode,
      component,
      config.theme || {},
    );

  if (!(window as any).Intl) {
    import('intl')
      // TODO(choko): Mapping on translations causes all locales to be prepared due to the way
      // webpack handles context import - look into whether it can be worked around.
      .then(() => import('intl/locale-data/jsonp/en.js'))
      .then(() => doRender(config.component))
      .catch((err) => {
        throw err;
      });
  } else {
    doRender(config.component);
  }

  return (component: React.ComponentClass | React.StatelessComponent) => {
    ReactDOM.unmountComponentAtNode(mountNode);
    doRender(component);
  };
}
