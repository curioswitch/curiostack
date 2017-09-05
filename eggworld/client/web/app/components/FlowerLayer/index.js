/**
*
* FlowerLayer
*
*/

// @flow

import type { Ingredient } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import React from 'react';
import { Group, Layer } from 'react-konva';

import KonvaImage from 'components/KonvaImage';

import { INGREDIENTS_MAP } from 'containers/HomePage/constants';

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

type Props = {
  eatenFood: Ingredient[],
};

class FlowerLayer extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <Layer>
        {this.props.eatenFood.map((ingredient, i) => (
          <Group
            key={ingredient}
            x={FLOWER_LOCATIONS[i].x}
            y={FLOWER_LOCATIONS[i].y}
          >
            <KonvaImage
              src={flowerImageSrc}
              width={200}
              height={200}
            />
            <KonvaImage
              src={INGREDIENTS_MAP[ingredient].imageSrc}
              x={30}
              y={30}
              width={146}
              height={146}
            />
          </Group>
        ))}
      </Layer>
    );
  }
}

export default FlowerLayer;
