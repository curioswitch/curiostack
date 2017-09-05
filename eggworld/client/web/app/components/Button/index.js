/**
*
* Button
*
*/

// @flow

import React from 'react';
import { Group, Text } from 'react-konva';

import KonvaImage from 'components/KonvaImage';

import buttonPressedSrc from './assets/button_pressed.png';
import buttonUnpressedSrc from './assets/button_unpressed.png';

type Props = {
  selected: boolean,
  x: number,
  y: number,
  label: string,
  onClick: () => void,
};

class Button extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <Group
        x={this.props.x}
        y={this.props.y}
        width={362}
        height={200}
        onClick={this.props.onClick}
      >
        <KonvaImage
          src={this.props.selected ? buttonPressedSrc : buttonUnpressedSrc}
          width={362}
          height={100}
        />
        <Text
          y={30}
          width={362}
          height={100}
          align="center"
          text={this.props.label}
          fontSize={40}
          fontFamily="Arial"
          fill="black"
        />
      </Group>
    );
  }
}

export default Button;
