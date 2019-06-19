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
 *
 */

import { Marker } from 'google-maps-react';
import { List, Record } from 'immutable';

import { SheetVisibility } from '../../components/BottomSheet';
import { Place } from '../../models';

import { Actions, ActionTypes } from './actions';

export interface StateProps {
  map?: google.maps.Map;
  landmarks: List<google.maps.places.PlaceResult>;
  places: List<Place>;
  selectedMarker?: Marker;
  bottomSheetVisibility: SheetVisibility;
}

export type State = Readonly<StateProps> & Record<StateProps>;

export const initialState = Record<StateProps>({
  map: undefined,
  landmarks: List(),
  places: List(),
  selectedMarker: undefined,
  bottomSheetVisibility: SheetVisibility.HIDDEN,
})();

export default function(state: State, action: Actions): State {
  switch (action.type) {
    case ActionTypes.GET_LANDMARKS_RESPONSE:
      return state.set('landmarks', List(action.payload));
    case ActionTypes.GET_PLACES_RESPONSE:
      return state.merge({
        places: List(action.payload),
        bottomSheetVisibility:
          action.payload.length > 0
            ? SheetVisibility.CLOSED
            : SheetVisibility.HIDDEN,
      });
    case ActionTypes.SET_BOTTOM_SHEET_OPEN:
      return state.set(
        'bottomSheetVisibility',
        action.payload ? SheetVisibility.OPEN : SheetVisibility.CLOSED,
      );
    case ActionTypes.SET_MAP:
      return state.set('map', action.payload);
    default:
      return state;
  }
}
