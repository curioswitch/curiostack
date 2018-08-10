/*
 * MIT License
 *
 * Copyright (c) 2018 Choko (choko@curioswitch.org)
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

import { Ingredient } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { Set } from 'immutable';
import React from 'react';
import { Group, Layer } from 'react-konva';

import { INGREDIENTS_MAP } from '../../containers/HomePage/constants';
import KonvaImage from '../KonvaImage';

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

interface Props {
  eatenFood: Set<Ingredient>;
  visible: boolean;
}

export default class FlowerLayer extends React.PureComponent<Props> {
  public render() {
    return (
      <Layer visible={this.props.visible}>
        {this.props.eatenFood.toIndexedSeq().map((ingredient, i) => (
          <Group
            key={ingredient}
            x={FLOWER_LOCATIONS[i].x}
            y={FLOWER_LOCATIONS[i].y}
          >
            <KonvaImage src={flowerImageSrc} width={200} height={200} />
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
