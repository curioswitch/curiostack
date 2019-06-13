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

import { Set } from 'immutable';
import React from 'react';
import { Layer } from 'react-konva';

import { IngredientDescriptor } from '../../containers/HomePage/constants';
import Food, { FoodDraggedHandler } from '../Food';

interface Props {
  eatenFood: Set<number>;
  ingredients: IngredientDescriptor[];
  onFoodDragged: FoodDraggedHandler;
  usableFood: Set<number>;
  visible: boolean;
}

const FoodLayer: React.FunctionComponent<Props> = (props) => {
  const startingX = 20;
  const deltaX = 380;
  const topRowY = 800;
  const bottomRowY = 1150;
  return (
    <Layer visible={props.visible}>
      {props.ingredients.map(({ key, name, imageSrc }, i) => (
        <Food
          key={key}
          ingredient={key}
          x={startingX + (i % 3) * deltaX}
          y={i < 3 ? topRowY : bottomRowY}
          imageSrc={imageSrc}
          name={name}
          removed={props.eatenFood.includes(key)}
          unusable={!props.usableFood.includes(key)}
          onFoodDragged={props.onFoodDragged}
        />
      ))}
    </Layer>
  );
};

export default React.memo(FoodLayer);
