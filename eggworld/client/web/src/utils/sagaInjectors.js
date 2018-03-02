// @flow

import type { AsyncStore } from '../store';

import {
  DAEMON,
  ONCE_TILL_UNMOUNT,
  RESTART_ON_REMOUNT,
} from './constants';

export function injectSagaFactory(store: AsyncStore) {
  return function injectSaga(
    key: string,
    descriptor: { saga: any, mode: RESTART_ON_REMOUNT | DAEMON | ONCE_TILL_UNMOUNT },
    args: any) {
    const newDescriptor = { ...descriptor, mode: descriptor.mode || RESTART_ON_REMOUNT };
    const { saga, mode } = newDescriptor;

    let hasSaga = Reflect.has(store.injectedSagas, key);

    if (process.env.NODE_ENV !== 'production') {
      const oldDescriptor = store.injectedSagas[key];
      // enable hot reloading of daemon and once-till-unmount sagas
      if (hasSaga && oldDescriptor.saga !== saga) {
        oldDescriptor.task.cancel();
        hasSaga = false;
      }
    }

    if (!hasSaga || (hasSaga && mode !== DAEMON && mode !== ONCE_TILL_UNMOUNT)) {
      store.injectedSagas[key] = { ...newDescriptor, task: store.runSaga(saga, args) }; // eslint-disable-line no-param-reassign
    }
  };
}

export function ejectSagaFactory(store: AsyncStore) {
  return function ejectSaga(key: string) {
    if (Reflect.has(store.injectedSagas, key)) {
      const descriptor = store.injectedSagas[key];
      if (descriptor.mode !== DAEMON) {
        descriptor.task.cancel();
        // Clean up in production; in development we need `descriptor.saga` for hot reloading
        if (process.env.NODE_ENV === 'production') {
          // Need some value to be able to detect `ONCE_TILL_UNMOUNT` sagas in `injectSaga`
          store.injectedSagas[key] = 'done'; // eslint-disable-line no-param-reassign
        }
      }
    }
  };
}

export default function getInjectors(store: AsyncStore) {
  return {
    injectSaga: injectSagaFactory(store),
    ejectSaga: ejectSagaFactory(store),
  };
}
