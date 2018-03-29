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

import React from 'react';
import { Group, Text } from 'react-konva';

import KonvaImage from '../KonvaImage';

import buttonPressedSrc from './assets/button_pressed.png';
import buttonUnpressedSrc from './assets/button_unpressed.png';

interface Props {
  selected: boolean;
  x: number;
  y: number;
  label: string;
  onClick: () => void;
}

export default class Button extends React.PureComponent<Props> {
  public render() {
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
