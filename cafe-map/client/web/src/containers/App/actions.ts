/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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

import { ActionsUnion, createAction } from '@curiostack/base-web';
import { LOCATION_CHANGE } from 'connected-react-router';
import { bindActionCreators, Dispatch } from 'redux';

export enum ActionTypes {
  SET_GLOBAL_ERROR_MESSAGE = 'App/SET_GLOBAL_ERROR_MESSAGE',
}

export const Actions = {
  locationChange: () => createAction(LOCATION_CHANGE),
  setGlobalErrorMessage: (errorMessage: string) =>
    createAction(ActionTypes.SET_GLOBAL_ERROR_MESSAGE, errorMessage),
};

export type Actions = ActionsUnion<typeof Actions>;

export type DispatchProps = typeof Actions;

export function mapDispatchToProps(dispatch: Dispatch<Actions>): DispatchProps {
  return bindActionCreators(Actions, dispatch);
}
