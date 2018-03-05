/**
 * app.js
 *
 * This is the entry file for the application, only setup and boilerplate
 * code.
 */

import 'sanitize.css/sanitize.css';
import React from 'react';
import ReactDOM from 'react-dom';
import initApp from '@curiostack/base-web/app/init';

import enMessages from './translations/en.json';

import {
  initialState as homePageInitialState
} from './containers/HomePage/reducer';

// Import root app
import App from 'containers/App';

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
