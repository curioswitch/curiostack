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
import { bindActionCreators, Dispatch } from 'redux';

import { GetPlaceResponse } from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

export enum ActionTypes {
  GET_PLACE = 'HomePage/GET_PLACE',
  GET_PLACE_RESPONSE = 'HomePage/GET_PLACE_RESPONSE',
}

export const Actions = {
  doGetPlace: (id: string) => createAction(ActionTypes.GET_PLACE, id),
  doGetPlaceResponse: (response: GetPlaceResponse) =>
    createAction(ActionTypes.GET_PLACE_RESPONSE, response),
  // TODO(choko): Fix typing so a no-payload action isn't required.
  dummy: () => createAction('dummy'),
};

export type Actions = ActionsUnion<typeof Actions>;

export type DispatchProps = typeof Actions;

export function mapDispatchToProps(dispatch: Dispatch<Actions>): DispatchProps {
  return bindActionCreators(Actions, dispatch);
}
