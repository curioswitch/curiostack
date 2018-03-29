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

import { createLocation } from 'history';
import { Record } from 'immutable';
import {
  LOCATION_CHANGE,
  LocationChangeAction,
  RouterState,
} from 'react-router-redux';
import { Reducer, ReducersMapObject } from 'redux';
import { combineReducers } from 'redux-immutable';
import { InjectableStore } from './store';

export interface RouterStateRecord extends Record<RouterState>, RouterState {}

export const routeInitialState: RouterStateRecord = Record<RouterState>({
  location: createLocation(''),
})();

export function routeReducer(
  state: RouterStateRecord,
  action: LocationChangeAction,
): RouterStateRecord {
  switch (action.type) {
    case LOCATION_CHANGE:
      return state.set('location', action.payload);
    default:
      return state;
  }
}

export function createInitalReducer(
  identityReducers: ReducersMapObject,
  nonInjectedReducers: ReducersMapObject,
): Reducer<any> {
  return combineReducers({
    ...identityReducers,
    ...nonInjectedReducers,
  });
}

export default function createReducer(
  store: InjectableStore<any>,
): Reducer<any> {
  return combineReducers({
    ...store.identityReducers,
    ...store.injectedReducers,
    ...store.nonInjectedReducers,
  });
}
