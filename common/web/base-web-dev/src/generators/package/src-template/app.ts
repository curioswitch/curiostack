import initApp, { GlobalStateBase } from '@curiostack/base-web';

import enMessages from './translations/en.json';

import {
  initialState as appInitialState,
  State as AppState,
} from './containers/App/reducer';
import {
  initialState as homePageInitialState,
  State as HomePageState,
} from './containers/HomePage/reducer';

import App from './containers/App';

import './global-styles';

interface OwnGlobalState {
  app: AppState;
  homePage: HomePageState;
}

export type GlobalState = GlobalStateBase & OwnGlobalState;

const initialState: OwnGlobalState = {
  app: appInitialState,
  homePage: homePageInitialState,
};

initApp({
  initialState,
  theme: {},
  component: App as any,
  messages: {
    en: enMessages,
  },
  defaultLocale: 'en',
});
