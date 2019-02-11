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
  ProvidedProps,
} from 'google-maps-react';
import React from 'react';

import CONFIG from '../../config';

type Props = ProvidedProps;

function initMap(_props?: MapProps, map?: google.maps.Map) {
  if (!map) {
    return;
  }
  map.setOptions({
    styles: [
      {
        featureType: 'administrative.locality',
        elementType: 'labels.text',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'geometry.fill',
        stylers: [
          {
            color: '#fefffd',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'labels',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'labels.icon',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'labels.text',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'labels.text.fill',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'landscape',
        elementType: 'labels.text.stroke',
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
            color: '#ffd26d',
          },
          {
            weight: 2,
          },
        ],
      },
      {
        featureType: 'road',
        elementType: 'geometry.stroke',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'transit.line',
        elementType: 'geometry.fill',
        stylers: [
          {
            weight: 4.5,
          },
        ],
      },
      {
        featureType: 'transit.line',
        elementType: 'labels.text',
        stylers: [
          {
            visibility: 'off',
          },
        ],
      },
      {
        featureType: 'transit.station',
        elementType: 'geometry.fill',
        stylers: [
          {
            color: '#ffccd0',
          },
          {
            weight: 0.5,
          },
        ],
      },
    ],
  });
}

const MapContainer: React.FunctionComponent<Props> = React.memo((props) => {
  const { google } = props;
  return (
    <Map
      onReady={initMap}
      google={google}
      zoom={12}
      centerAroundCurrentLocation={true}
    />
  );
});

export default GoogleApiWrapper({
  apiKey: CONFIG.google.apiKey,
})(MapContainer);
