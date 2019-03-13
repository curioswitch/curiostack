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

import { formValueSelector } from 'redux-form/immutable';
import {
  all,
  AllEffect,
  call,
  put,
  select,
  takeLatest,
  throttle,
} from 'redux-saga/effects';

import {
  GetPlacesRequest,
  GetPlacesResponse,
} from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

import ApiClient from '../../utils/api-client';

import { GlobalState } from '../../state';
import { Actions, ActionTypes } from './actions';
import { selectMap } from './selectors';

function* getLandmarks() {
  const map: google.maps.Map | undefined = yield select(selectMap);
  if (!map) {
    return;
  }
  const places = new google.maps.places.PlacesService(map);

  const results: google.maps.places.PlaceResult[] = yield call(
    () =>
      new Promise<google.maps.places.PlaceResult[]>((resolve, reject) => {
        places.nearbySearch(
          {
            bounds: map.getBounds()!,
            type: 'park',
          },
          (res, status) => {
            if (status !== google.maps.places.PlacesServiceStatus.OK) {
              reject(new Error(`Error searching for places: ${status}`));
            } else {
              resolve(res);
            }
          },
        );
      }),
  );

  yield put(Actions.getLandmarksResponse(results));
}

function* getPlaces() {
  const client = new ApiClient();

  const response: GetPlacesResponse = yield call(() =>
    client.getPlaces(new GetPlacesRequest()),
  );
  yield put(Actions.getPlacesResponse(response));
}

const selectSearchForm = (state: GlobalState) =>
  formValueSelector('homePage')(state, 'query');

function* search() {
  const map: google.maps.Map | undefined = yield select(selectMap);
  if (!map) {
    return;
  }
  const places = new google.maps.places.PlacesService(map);

  const query: string = yield select(selectSearchForm);

  const results: google.maps.places.PlaceResult[] = yield call(
    () =>
      new Promise<google.maps.places.PlaceResult[]>((resolve, reject) => {
        places.textSearch(
          {
            query,
            bounds: map.getBounds()!,
            type: 'transit_station',
          },
          (res, status) => {
            if (status !== google.maps.places.PlacesServiceStatus.OK) {
              reject(new Error(`Error searching for places: ${status}`));
            } else {
              resolve(res);
            }
          },
        );
      }),
  );

  if (results.length > 0) {
    const place = results[0];
    map.fitBounds(place.geometry!.viewport);
  }
}

export default function* rootSaga(): IterableIterator<AllEffect<{}>> {
  yield all([
    takeLatest(ActionTypes.GET_PLACES, getPlaces),
    takeLatest(ActionTypes.SEARCH, search),
    throttle(2000, ActionTypes.GET_LANDMARKS, getLandmarks),
  ]);
}
