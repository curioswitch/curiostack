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

import { Filters, Group as GroupImpl, Node } from 'konva';
import React from 'react';
import { Group, Text } from 'react-konva';

import KonvaImage from '../KonvaImage';

export type FoodDraggedHandler = (ingredient: Ingredient, node: Node) => void;

interface Props {
  onFoodDragged: FoodDraggedHandler;
  ingredient: number;
  x: number;
  y: number;
  imageSrc: string;
  name: string;
  removed: boolean;
  unusable: boolean;
}

export default class Food extends React.PureComponent<Props> {
  private node?: GroupImpl;
  private removed = false;

  public componentWillReceiveProps(nextProps: Props) {
    if (!this.node || this.removed) {
      return;
    }
    const node = this.node;
    if (nextProps.removed) {
      // Programatically remove the eaten food instead of declaratively because we do not manage
      // the food's position in our state and cannot allow it to be rerendered.
      const drawingNode = node.getLayer() || node.getStage();
      node.remove();
      drawingNode.batchDraw();
      this.removed = true;
    }
    if (nextProps.unusable) {
      node.draggable(false);
      const image = node.getChildren()[0];
      image.cache();
      image.filters([Filters.Grayscale]);
    }
  }

  // Draggable node position is managed by konva so we cannot rerender them.
  // Ideally, we could make the nodes draggable without Konva rerendering them, which would
  // allow the positions to be managed in our state, but this is not possible.
  public shouldComponentUpdate() {
    return false;
  }

  public render() {
    const { ingredient } = this.props;
    return (
      <Group
        key={ingredient}
        x={this.props.x}
        y={this.props.y}
        width={292}
        height={292}
        draggable
        onDragMove={this.handleDragMove}
        ref={(node: any) => {
          this.node = node;
        }}
      >
        <KonvaImage src={this.props.imageSrc} width={292} height={292} />
        <Text
          x={0}
          y={300}
          width={292}
          height={292}
          align="center"
          text={this.props.name}
          fontSize={50}
          fontFamily="Arial"
          fill="black"
        />
      </Group>
    );
  }

  private handleDragMove = (e: any) =>
    this.props.onFoodDragged(this.props.ingredient, e.target);
}
