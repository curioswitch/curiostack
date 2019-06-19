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

/* global google */

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
  LatLngBounds,
  LatLng,
} from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

import ApiClient from '../../utils/api-client';

import { GlobalState } from '../../state';
import { Actions, ActionTypes } from './actions';
import { selectMap } from './selectors';
import { Place } from '../../models';

import PlaceResult = google.maps.places.PlaceResult;

const LANDMARK_TYPES = new Set([
  'airport',
  'amusement_park',
  'bar',
  'book_store',
  'bus_station',
  'convenience_store',
  'doctor',
  'electronics_store',
  'fire_station',
  'gas_station',
  'hair_care',
  'hospital',
  'park',
  'parking',
  'pet_store',
  'police',
  'post_office',
  'school',
  'stadium',
  'taxi_stand',
  'zoo',
]);

interface PlaceSearchResponse {
  results: google.maps.places.PlaceResult[];
  pagination: google.maps.places.PlaceSearchPagination;
}

async function nearbySearch(
  places: google.maps.places.PlacesService,
  bounds: google.maps.LatLngBounds,
  type?: string,
): Promise<PlaceSearchResponse> {
  return new Promise<PlaceSearchResponse>((resolve, reject) => {
    places.nearbySearch(
      {
        bounds,
        type,
      } as any,
      (results, status, pagination) => {
        if (status !== google.maps.places.PlacesServiceStatus.OK) {
          reject(new Error(`Error searching for places: ${status}`));
        } else {
          resolve({
            results,
            pagination,
          });
        }
      },
    );
  });
}

async function placeDetails(
  places: google.maps.places.PlacesService,
  placeId: string,
): Promise<PlaceResult> {
  return new Promise<PlaceResult>((resolve, reject) => {
    places.getDetails(
      {
        placeId,
        fields: ['geometry', 'photo', 'place_id'],
      },
      (result, status) => {
        if (status !== google.maps.places.PlacesServiceStatus.OK) {
          reject(new Error(`Error fetching place details: ${status}`));
        } else {
          resolve(result);
        }
      },
    );
  });
}

function* getLandmarks() {
  const map: google.maps.Map | undefined = yield select(selectMap);
  if (!map) {
    return;
  }
  const places = new google.maps.places.PlacesService(map);
  const bounds = map.getBounds()!;

  const landmarks: google.maps.places.PlaceResult[] = [];
  const seenIds: Set<string> = new Set();

  for (let i = 0; i < 1; i += 1) {
    /* if (landmarks.length >= 10) {
      yield put(Actions.getLandmarksResponse(landmarks));
      return;
    } */

    const response: PlaceSearchResponse = yield call(() =>
      nearbySearch(places, bounds),
    );
    for (const place of response.results) {
      if (seenIds.has(place.place_id!)) {
        continue;
      }
      seenIds.add(place.place_id!);
      for (const type of place.types!) {
        if (LANDMARK_TYPES.has(type)) {
          landmarks.push(place);
          break;
        }
      }
    }
  }

  for (const t of [
    'beauty_salon',
    'gas_station',
    'park',
    'book_store',
    'hospital',
    'post_office',
    'school',
  ]) {
    let response: PlaceSearchResponse;
    try {
      response = yield call(() => nearbySearch(places, bounds, t));
    } catch (e) {
      continue;
    }
    for (const place of response.results) {
      if (seenIds.has(place.place_id!)) {
        continue;
      }
      seenIds.add(place.place_id!);
      for (const type of place.types!) {
        if (LANDMARK_TYPES.has(type)) {
          landmarks.push(place);
          break;
        }
      }
    }
  }

  yield put(Actions.getLandmarksResponse(landmarks.slice(0, 90)));
}

function* getPlaces() {
  const map: google.maps.Map | undefined = yield select(selectMap);
  if (!map) {
    return;
  }

  const placesService = new google.maps.places.PlacesService(map);

  const client = new ApiClient();

  const bounds = map.getBounds()!;

  const requestViewport = new LatLngBounds();

  const northEast = new LatLng();
  northEast.setLatitude(bounds.getNorthEast().lat());
  northEast.setLongitude(bounds.getNorthEast().lng());
  requestViewport.setNorthEast(northEast);

  const southWest = new LatLng();
  southWest.setLatitude(bounds.getSouthWest().lat());
  southWest.setLongitude(bounds.getSouthWest().lng());
  requestViewport.setSouthWest(southWest);

  const request = new GetPlacesRequest();
  request.setViewport(requestViewport);

  const response: GetPlacesResponse = yield call(() =>
    client.getPlaces(request),
  );

  const placeResults: google.maps.places.PlaceResult[] = yield call(() =>
    Promise.all(
      response
        .getPlaceList()
        .slice(0, 5)
        .map((place) => placeDetails(placesService, place.getGooglePlaceId())),
    ),
  );

  const places = placeResults.map(
    (result, i) => new Place(response.getPlaceList()[i], result),
  );

  yield put(Actions.getPlacesResponse(places));
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
