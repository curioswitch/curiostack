// @flow

import type { Reducer } from 'redux';

import invariant from 'invariant';
import isEmpty from 'lodash/isEmpty';

import type { AsyncStore } from '../store';
import createReducer from '../reducers';

export function injectReducerFactory(store: AsyncStore) {
  return function injectReducer(key: string, reducer: Reducer<*, *>) {
    invariant(
      !isEmpty(key),
      '(app/utils...) injectReducer: Expected `key` to be non-empty'
    );

    // Check `store.injectedReducers[key] === reducer` for hot reloading when a key is the same but a reducer is different
    if (Reflect.has(store.injectedReducers, key) && store.injectedReducers[key] === reducer) return;

    store.injectedReducers[key] = reducer; // eslint-disable-line no-param-reassign
    store.replaceReducer(createReducer(store.injectedReducers));
  };
}

export default function getInjectors(store: AsyncStore) {
  return {
    injectReducer: injectReducerFactory(store),
  };
}
