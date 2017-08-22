/**
*
* FoodLayer
*
*/

// @flow

import type { Set } from 'immutable';
import type { Node } from 'konva';

import React from 'react';
import { Group, Layer, Text } from 'react-konva';

import KonvaImage from 'components/KonvaImage';

class FoodLayer extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  props: {
    ingredients: Array<{ key: string, name: string, imageSrc: string }>,
    onFoodDragged: (Node) => void,
  };

  render() {
    const { ingredients, ...others } = this.props;
    const startingX = 20;
    const deltaX = 380;
    const topRowY = 800;
    const bottomRowY = 1150;
    const width = 292;
    const height = 292;
    return (
      <Layer {...others}>
        {ingredients.map(({ key, name, imageSrc }, i) => (
          <Group
            key={key}
            x={startingX + ((i % 3) * deltaX)}
            y={(i < 3) ? topRowY : bottomRowY}
            width={width}
            height={height}
            draggable
            onDragmove={(e) => this.props.onFoodDragged({
              ingredient: key,
              node: e.target,
            })}
          >
            <KonvaImage
              src={imageSrc}
              width={width}
              height={height}
            />
            <Text
              x={0}
              y={300}
              width={width}
              height={height}
              align="center"
              text={name}
              fontSize={50}
              fontFamily="Arial"
              fill="black"
            />
          </Group>
        ))}
      </Layer>
    );
  }
}

export default FoodLayer;
