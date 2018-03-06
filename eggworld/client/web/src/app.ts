/**
 * app.ts
 *
 * This is the entry file for the application, only setup and boilerplate
 * code.
 */

import initApp from '@curiostack/base-web/app/init';
import 'sanitize.css/sanitize.css';

import enMessages from './translations/en.json';

import { initialState as homePageInitialState } from './containers/HomePage/reducer';

// Import root app
// tslint:disable-next-line:no-var-requires
const App = require('containers/App').default;

const initialState = {
  homePage: homePageInitialState,
};

const render = initApp({
  initialState,
  component: App,
  messages: {
    en: enMessages,
  },
  defaultLocale: 'en',
});

if (module.hot) {
  module.hot.accept(['containers/App'], () => {
    render(App);
  });
}
