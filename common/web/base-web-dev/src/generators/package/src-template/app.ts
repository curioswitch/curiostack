/**
 * app.ts
 *
 * This is the entry file for the application, only setup and boilerplate
 * code.
 */

// TODO(anuraag): Only import used plugins, for some reason the workaround is not working.
// https://github.com/googleanalytics/autotrack/issues/137
import 'autotrack/autotrack';

import initApp, { GlobalStateBase } from '@curiostack/base-web';

import enMessages from './translations/en.json';

// Import all the third party stuff
import FontFaceObserver from 'fontfaceobserver';

import {
  initialState as amateurHomePageInitialState,
  State as AmateurHomePageState,
} from 'containers/AmateurHomePage/reducer';
import {
  initialState as antennaPageInitialState,
  State as AntennaPageState,
} from 'containers/AntennaPage/reducer';
import {
  initialState as appInitialState,
  State as AppState,
} from 'containers/App/reducer';
import {
  initialState as forgotPasswordPageInitialState,
  State as ForgotPasswordState,
} from 'containers/ForgotPasswordPage/reducer';
import {
  initialState as joinNetworkPageInitialState,
  State as JoinNetworkState,
} from 'containers/JoinNetworkPage/reducer';
import {
  initialState as requestTrackingPageInitialState,
  State as RequestTrackingPageState,
} from 'containers/RequestTrackingPage/reducer';
import {
  initialState as satellitePageInitialState,
  State as SatellitePageState,
} from 'containers/SatellitePage/reducer';
import {
  initialState as satelliteSearchPageInitialState,
  State as SatelliteSearchPageState,
} from 'containers/SatelliteSearchPage/reducer';

// Import root app
import App from 'containers/App';

// Import CSS reset and Global Styles
import { Map } from 'immutable';

import theme from './theme';

import './global-styles';

const korolevMediumObserver = new FontFaceObserver('Korolev-Medium', {});
const korolevBoldObserver = new FontFaceObserver('Korolev-Bold', {});

Promise.all([korolevMediumObserver.load(), korolevBoldObserver.load()])
  .then(() => {
    document.documentElement.className += ' fonts-loaded';
    // Optimistically mark whether fonts have ever been loaded. If a font is removed from cache, it will cause
    // FOIT, but this should be rare and is not a disaster.
    window.sessionStorage.fontsLoaded = true;
  })
  .catch(() => {
    delete window.sessionStorage.fontsLoaded;
  });

interface OwnGlobalState {
  app: AppState;
  form: Map<string, any>;
  amateurHomePage: AmateurHomePageState;
  antennaPage: AntennaPageState;
  forgotPasswordPage: ForgotPasswordState;
  joinNetworkPage: JoinNetworkState;
  requestTrackingPage: RequestTrackingPageState;
  satellitePage: SatellitePageState;
  satelliteSearchPage: SatelliteSearchPageState;
}

export type GlobalState = GlobalStateBase & OwnGlobalState;

const initialState: OwnGlobalState = {
  app: appInitialState,
  form: Map(),

  amateurHomePage: amateurHomePageInitialState,
  antennaPage: antennaPageInitialState,
  forgotPasswordPage: forgotPasswordPageInitialState,
  joinNetworkPage: joinNetworkPageInitialState,
  requestTrackingPage: requestTrackingPageInitialState,
  satellitePage: satellitePageInitialState,
  satelliteSearchPage: satelliteSearchPageInitialState,
};

initApp({
  initialState,
  theme,
  // TODO(anuraag): Figure out why this cast is needed.
  component: App as any,
  messages: {
    en: enMessages,
  },
  defaultLocale: 'en',
});

const ga = (window as any).ga;
ga('create', 'UA-81424338-2', 'auto');
ga('require', 'eventTracker', { attributePrefix: 'data-ga-' });
ga('require', 'outboundLinkTracker');
