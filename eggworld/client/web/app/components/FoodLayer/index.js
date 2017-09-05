/**
*
* FoodLayer
*
*/

// @flow

import React from 'react';
import { Layer } from 'react-konva';

import Food from 'components/Food';

type Props = {
  eatenFood: number[],
  ingredients: Array<{ key: number, name: string, imageSrc: string }>,
  onFoodDragged: (any) => void,
  usableFood: number[],
  visible: boolean,
};

class FoodLayer extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  render() {
    const startingX = 20;
    const deltaX = 380;
    const topRowY = 800;
    const bottomRowY = 1150;
    return (
      <Layer visible={this.props.visible}>
        {this.props.ingredients.map(({ key, name, imageSrc }, i) => (
          <Food
            key={key}
            ingredient={key}
            x={startingX + ((i % 3) * deltaX)}
            y={(i < 3) ? topRowY : bottomRowY}
            imageSrc={imageSrc}
            name={name}
            removed={this.props.eatenFood.includes(key)}
            unusable={!this.props.usableFood.includes(key)}
            onFoodDragged={this.props.onFoodDragged}
          />
        ))}
      </Layer>
    );
  }
}

export default FoodLayer;
