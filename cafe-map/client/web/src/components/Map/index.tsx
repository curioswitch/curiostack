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

import {
  GoogleApiWrapper,
  Map,
  MapProps,
  Marker,
  ProvidedProps,
} from 'google-maps-react';
import { List } from 'immutable';
import React, { useCallback } from 'react';

import { Place } from '@curiostack/cafemap-api/org/curioswitch/cafemap/api/cafe-map-service_pb';

import CONFIG from '../../config';

import lawsonSvg from './images/lawson.svg';
import pinkMarkerSvg from './images/pink-marker.svg';
import sevenElevenSvg from './images/seven-eleven.svg';
import treeSvg from './images/park.svg';

interface OwnProps {
  doGetLandmarks: () => void;
  doSetMap: (map: google.maps.Map) => void;

  onOpenPlace: (id: string) => void;

  landmarks: List<google.maps.places.PlaceResult>;
  places: List<Place>;
}

type Props = ProvidedProps & OwnProps;

const TEST_PLACES = [
  {
    title: 'セブンイレブン',
    lat: 35.5517657,
    lng: 139.6741667,
    icon: sevenElevenSvg,
  },
  {
    title: 'ローソン',
    lat: 35.5511861,
    lng: 139.6725258,
    icon: lawsonSvg,
  },
];

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
    doGetLandmarks,
    doSetMap,
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
      onIdle={doGetLandmarks}
      onReady={onMapReady}
      google={google}
      zoom={12}
      centerAroundCurrentLocation={true}
    >
      {places.map((place) => (
        <Marker
          key={place.getInstagramId()}
          title={place.getName()}
          position={{
            lat: place.getPosition().getLatitude(),
            lng: place.getPosition().getLongitude(),
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
            url: treeSvg,
            scaledSize: new google.maps.Size(45, 45),
          }}
        />
      ))}
      {TEST_PLACES.map((place) => (
        <Marker
          title={place.title}
          position={{
            lat: place.lat,
            lng: place.lng,
          }}
          icon={{
            url: place.icon,
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
})(MapContainer);
