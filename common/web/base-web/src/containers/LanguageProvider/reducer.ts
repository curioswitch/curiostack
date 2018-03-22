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
 * LanguageProvider reducer
 *
 */

import { Record } from 'immutable';
import { getType } from 'typesafe-actions';

import * as actions from './actions';

export interface LanguageState {
  readonly locale: string;
}

export interface LanguageStateRecord
  extends Record<LanguageState>,
    LanguageState {}

export const initialState: LanguageStateRecord = Record<LanguageState>({
  locale: 'en',
})();

export default function reducer(
  state: LanguageStateRecord,
  action: Actions,
): LanguageStateRecord {
  switch (action.type) {
    case getType(actions.changeLocale):
      return state.set('locale', action.payload);
    default:
      return state;
  }
}

// inferring union type of actions
import { $call } from 'utility-types';
const returnsOfActions = Object.values(actions).map($call);
type Actions = typeof returnsOfActions[number];
