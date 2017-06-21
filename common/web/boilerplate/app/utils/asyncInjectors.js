// @flow

import type { Reducer } from 'redux';

import type { AsyncStore } from 'store';

import conformsTo from 'lodash/conformsTo';
import isEmpty from 'lodash/isEmpty';
import isFunction from 'lodash/isFunction';
import isObject from 'lodash/isObject';
import isString from 'lodash/isString';
import invariant from 'invariant';
import warning from 'warning';
import createReducer from 'reducers';

/**
 * Validate the shape of redux store
 */
export function checkStore(store: AsyncStore) {
  const shape = {
    dispatch: isFunction,
    subscribe: isFunction,
    getState: isFunction,
    replaceReducer: isFunction,
    runSaga: isFunction,
    asyncReducers: isObject,
  };
  invariant(
    conformsTo(store, shape),
    '(app/utils...) asyncInjectors: Expected a valid redux store'
  );
}

/**
 * Inject an asynchronously loaded reducer
 */
export function injectAsyncReducer(store: AsyncStore, isValid: boolean) {
  return function injectReducer(name: string, asyncReducer: Reducer<*, *>) {
    if (!isValid) checkStore(store);

    invariant(
      isString(name) && !isEmpty(name) && isFunction(asyncReducer),
      '(app/utils...) injectAsyncReducer: Expected `asyncReducer` to be a reducer function'
    );

    if (Reflect.has(store.asyncReducers, name)) return;

    store.asyncReducers[name] = asyncReducer; // eslint-disable-line no-param-reassign
    store.replaceReducer(createReducer(store.asyncReducers));
  };
}

/**
 * Inject an asynchronously loaded saga
 */
export function injectAsyncSagas(store: AsyncStore, isValid: boolean) {
  return function injectSagas(sagas: Array<Generator<*, void, *>>) {
    if (!isValid) checkStore(store);

    invariant(
      Array.isArray(sagas),
      '(app/utils...) injectAsyncSagas: Expected `sagas` to be an array of generator functions'
    );

    warning(
      !isEmpty(sagas),
      '(app/utils...) injectAsyncSagas: Received an empty `sagas` array'
    );

    sagas.map(store.runSaga);
  };
}

/**
 * Helper for creating injectors
 */
export function getAsyncInjectors(store: AsyncStore) {
  checkStore(store);

  return {
    injectReducer: injectAsyncReducer(store, true),
    injectSagas: injectAsyncSagas(store, true),
  };
}

/**
 * Helper to log an error when asynchronous loading fails.
 */
export function errorLoading(err: Error) {
  /* istanbul ignore next */
  console.error('Dynamic page loading failed', err); // eslint-disable-line no-console
}
