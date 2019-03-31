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

/**
 * Create the store with asynchronously loaded reducers
 */

import {
  connectRouter,
  routerMiddleware,
} from 'connected-react-router/immutable';
import { History } from 'history';
import { Record } from 'immutable';
import {
  applyMiddleware,
  compose,
  createStore,
  Reducer,
  ReducersMapObject,
  Store,
} from 'redux';
import createSagaMiddleware from 'redux-saga';

import languageProviderReducer, {
  initialState as languageProviderInitialState,
  LanguageStateRecord,
} from '../containers/LanguageProvider/reducer';
import { GlobalStateBase } from './index';
import createReducer, {
  createInitalReducer,
  routeInitialState,
  RouterStateRecord,
} from './reducers';

const sagaMiddleware = createSagaMiddleware();

export interface InjectableStore<S = any> extends Store<S> {
  runSaga: any;
  identityReducers: ReducersMapObject;
  injectedReducers: ReducersMapObject;
  injectedSagas: any;
  nonInjectedReducers: ReducersMapObject;
  initialState: any;
}

function identityReducer<S>(state: S): S {
  return state;
}

export default function configureStore<S extends object>(
  initialState: S,
  history: History,
): InjectableStore<any> {
  const nonInjectedReducers = {
    language: languageProviderReducer as Reducer<LanguageStateRecord>,
    router: connectRouter(history) as Reducer<RouterStateRecord>,
  };

  // Create the store with two middlewares
  // 1. sagaMiddleware: Makes redux-sagas work
  // 2. routerMiddleware: Syncs the location/URL path to the state
  const middlewares = [sagaMiddleware, routerMiddleware(history)];

  const enhancers = [applyMiddleware(...middlewares)];

  const windowCast = window ? (window as any) : null;

  // If Redux DevTools Extension is installed use it, otherwise use Redux compose
  const composeEnhancers =
    process.env.NODE_ENV !== 'production' &&
    windowCast &&
    // eslint-disable-next-line no-underscore-dangle
    windowCast.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__
      ? // eslint-disable-next-line no-underscore-dangle
        windowCast.__REDUX_DEVTOOLS_EXTENSION_COMPOSE__({
          // TODO Try to remove when `react-router-redux` is out of beta, LOCATION_CHANGE should not be fired more than once after hot reloading
          // Prevent recomputing reducers for `replaceReducer`
          shouldHotReload: false,
        })
      : compose;

  type GlobalState = GlobalStateBase & S;
  type GlobalStateRecord = Record<GlobalState> & GlobalState;

  // NOTE(choko): Cannot use spread with a generic type yet - https://github.com/Microsoft/TypeScript/pull/13288
  // tslint:disable-next-line:prefer-object-spread
  const merged = Object.assign(
    {},
    {
      language: languageProviderInitialState,
      router: routeInitialState,
    },
    initialState,
  );
  const globalInitialState = Record<GlobalState>(merged)();

  const identityReducers = globalInitialState
    .toSeq()
    .keySeq()
    .filter((key) => !(key in Object.keys(nonInjectedReducers)))
    .reduce(
      (reducers, key) => ({
        ...reducers,
        [key]: identityReducer,
      }),
      {},
    );

  const store = (createStore(
    createInitalReducer(identityReducers, nonInjectedReducers, initialState),
    // TODO(choko): Figure out what's wrong with the types that causes the need for this force-cast.
    globalInitialState as any,
    composeEnhancers(...enhancers),
  ) as any) as InjectableStore<GlobalStateRecord>;

  // Extensions
  store.runSaga = sagaMiddleware.run;
  store.identityReducers = identityReducers;
  store.nonInjectedReducers = nonInjectedReducers;
  store.injectedReducers = {}; // Reducer registry
  store.injectedSagas = {}; // Saga registry
  store.initialState = globalInitialState;
  // Make reducers hot reloadable, see http://mxs.is/googmo
  /* istanbul ignore next */
  if (module.hot) {
    module.hot.accept('./reducers', () => {
      store.replaceReducer(createReducer(store));
    });
  }

  return store;
}
