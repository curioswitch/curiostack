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

/* eslint-disable @typescript-eslint/camelcase */

import {
  GoogleApiWrapper,
  Map,
  MapProps,
  Marker,
  ProvidedProps,
} from 'google-maps-react';
import { List, Map as ImmutableMap } from 'immutable';
import React, { useCallback } from 'react';

import { Place } from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

import CONFIG from '../../config';

import pinkMarkerSvg from './images/pink-marker.svg';

import airportSvg from './images/airport.svg';
import amusementParkSvg from './images/amusement_park.svg';
import barSvg from './images/bar.svg';
import beautySalonSvg from './images/beauty_salon.svg';
import bookStoreSvg from './images/book_store.svg';
import buildingSvg from './images/building.png';
import busStationSvg from './images/bus_station.svg';
import convenienceStoreSvg from './images/convenience_store.svg';
import doctorSvg from './images/doctor.svg';
import electroncisStoreSvg from './images/electronics_store.svg';
import fireStationSvg from './images/fire_station.svg';
import gasStationSvg from './images/gas_station.svg';
import hairCareSvg from './images/hair_care.svg';
import hospitalSvg from './images/hospital.svg';
import parkSvg from './images/park.svg';
import parkingSvg from './images/parking.svg';
import petStoreSvg from './images/pet_store.svg';
import policeSvg from './images/police.svg';
import postOfficeSvg from './images/post_office.svg';
import schoolSvg from './images/school.svg';
import stadiumSvg from './images/stadium.svg';
import taxiStandSvg from './images/taxi_stand.svg';
import zooSvg from './images/zoo.svg';

const LANDMARK_IMAGES = ImmutableMap({
  airport: airportSvg,
  amustment_park: amusementParkSvg,
  bar: barSvg,
  beauty_salon: beautySalonSvg,
  book_store: bookStoreSvg,
  building: buildingSvg,
  bus_station: busStationSvg,
  convenience_store: convenienceStoreSvg,
  doctor: doctorSvg,
  electronics_store: electroncisStoreSvg,
  fire_station: fireStationSvg,
  gas_station: gasStationSvg,
  hair_care: hairCareSvg,
  hospital: hospitalSvg,
  park: parkSvg,
  parking: parkingSvg,
  pet_store: petStoreSvg,
  police: policeSvg,
  post_office: postOfficeSvg,
  school: schoolSvg,
  stadium: stadiumSvg,
  taxi_stand: taxiStandSvg,
  zoo: zooSvg,
});

function landmarkImage(place: google.maps.places.PlaceResult): string {
  for (const type of place.types!) {
    const image = LANDMARK_IMAGES.get(type);
    if (image) {
      return image;
    }
  }
  return buildingSvg;
}

interface OwnProps {
  doUpdateMap: () => void;
  doSetMap: (map: google.maps.Map) => void;

  onOpenPlace: (id: string) => void;

  landmarks: List<google.maps.places.PlaceResult>;
  places: List<Place>;
}

type Props = ProvidedProps & OwnProps;

function initMap(map: google.maps.Map) {
  map.setOptions({
    disableDefaultUI: true,
    styles: [
      {
        elementType: 'labels',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'administrative.land_parcel',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'administrative.neighborhood',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'geometry.stroke',
        stylers: [
          {
            color: '#ffffff',
          },
        ],
      },
      {
        featureType: 'landscape.man_made',
        elementType: 'geometry.stroke',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'road',
        elementType: 'geometry.fill',
        stylers: [
          {
            color: '#ffffff',
          },
        ],
      },
      {
        featureType: 'road',
        elementType: 'geometry.stroke',
        stylers: [
          {
            color: '#ffffff',
          },
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'transit.line',
        stylers: [
          {
            weight: 1.5,
          },
        ],
      },
    ],
  });
}

const MapContainer: React.FunctionComponent<Props> = React.memo((props) => {
  const {
    doSetMap,
    doUpdateMap,
    google,
    landmarks,
    onOpenPlace,
    places,
  } = props;

  const onMapReady = useCallback((_props?: MapProps, map?: google.maps.Map) => {
    if (map) {
      doSetMap(map);
      initMap(map);
    }
  }, []);

  return (
    <Map
      onIdle={doUpdateMap}
      onReady={onMapReady}
      google={google}
      zoom={12}
      centerAroundCurrentLocation
    >
      {places.map((place) => (
        <Marker
          key={place.getInstagramId()}
          title={place.getName()}
          position={{
            lat: place.getPosition()!.getLatitude(),
            lng: place.getPosition()!.getLongitude(),
          }}
          onClick={
            // tslint:disable-next-line
            () => onOpenPlace(place.getInstagramId())
          }
          icon={{
            url: pinkMarkerSvg,
            scaledSize: new google.maps.Size(45, 45),
          }}
        />
      ))}
      {landmarks.map((place) => (
        <Marker
          title={place.name}
          position={place.geometry!.location}
          icon={{
            url: landmarkImage(place),
            scaledSize: new google.maps.Size(45, 45),
          }}
        />
      ))}
    </Map>
  );
});

export default GoogleApiWrapper({
  apiKey: CONFIG.google.apiKey,
  libraries: ['places'],
  language: 'ja',
})(MapContainer);
