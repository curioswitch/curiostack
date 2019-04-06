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

/* global google */

import { all, AllEffect, call, put, takeLatest } from 'redux-saga/effects';

import {
  GetPlaceRequest,
  GetPlaceResponse,
  LatLng,
} from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

import ApiClient from '../../utils/api-client';

import { Actions, ActionTypes } from './actions';

async function findPlace(
  places: google.maps.places.PlacesService,
  name: string,
  location: LatLng,
): Promise<google.maps.places.PlaceResult[]> {
  return new Promise<google.maps.places.PlaceResult[]>((resolve, reject) => {
    places.findPlaceFromQuery(
      {
        query: name,
        locationBias: {
          lat: location.getLatitude(),
          lng: location.getLongitude(),
        },
        fields: [
          'place_id',
          'formatted_address',
          'photos',
          'name',
          'opening_hours',
          'price_level',
        ],
      },
      (results, status) => {
        if (status !== google.maps.places.PlacesServiceStatus.OK) {
          reject(new Error(`Error searching for places: ${status}`));
        } else {
          resolve(results);
        }
      },
    );
  });
}

async function fetchPlace(
  places: google.maps.places.PlacesService,
  placeId: string,
): Promise<google.maps.places.PlaceResult> {
  return new Promise<google.maps.places.PlaceResult>((resolve, reject) => {
    places.getDetails(
      {
        placeId,
        fields: [
          'formatted_address',
          'formatted_phone_number',
          'photos',
          'name',
          'opening_hours',
          'price_level',
        ],
      },
      (result, status) => {
        if (status !== google.maps.places.PlacesServiceStatus.OK) {
          reject(new Error(`Error searching for places: ${status}`));
        } else {
          resolve(result);
        }
      },
    );
  });
}

function* getPlace({ payload: id }: ReturnType<typeof Actions.doGetPlace>) {
  const client = new ApiClient();

  const request = new GetPlaceRequest();
  request.setInstagramId(id);
  const response: GetPlaceResponse = yield call(() => client.getPlace(request));

  const attribution = document.createElement('div');
  const places = new google.maps.places.PlacesService(attribution);

  const googlePlaces: google.maps.places.PlaceResult[] = yield call(() =>
    findPlace(
      places,
      response.getPlace().getName(),
      response.getPlace().getPosition(),
    ),
  );

  const googlePlace: google.maps.places.PlaceResult = yield call(() =>
    fetchPlace(places, googlePlaces[0].place_id!),
  );

  yield put(
    Actions.doGetPlaceResponse({
      place: response.getPlace(),
      googlePlace,
    }),
  );
}

export default function* rootSaga(): IterableIterator<AllEffect<{}>> {
  yield all([takeLatest(ActionTypes.GET_PLACE, getPlace)]);
}
