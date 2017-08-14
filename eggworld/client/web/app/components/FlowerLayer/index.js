/**
*
* FlowerLayer
*
*/

// @flow

import React from 'react';
import { Layer } from 'react-konva';

import KonvaImage from 'components/KonvaImage';

import flowerImageSrc from './assets/flower1.png';

// Location of each flower an eaten food may be placed in.
const FLOWER_LOCATIONS = [
  {
    x: 50,
    y: 100,
  },
  {
    x: 30,
    y: 350,
  },
  {
    x: 150,
    y: 530,
  },
  {
    x: 650,
    y: 540,
  },
  {
    x: 850,
    y: 430,
  },
  {
    x: 750,
    y: 250,
  },
];

class FlowerLayer extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <Layer>
        {FLOWER_LOCATIONS.map(({ x, y }) => (
          <KonvaImage
            key={`${x}:${y}`}
            src={flowerImageSrc}
            x={x}
            y={y}
            width={200}
            height={200}
          />
        ))}
      </Layer>
    );
  }
}

export default FlowerLayer;
