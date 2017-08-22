/**
*
* Food
*
*/

// @flow

import React from 'react';
import { Group, Text } from 'react-konva';

import KonvaImage from 'components/KonvaImage';

type PropTypes = {
  onFoodDragged: (any) => void,
  ingredient: string,
  x: number,
  y: number,
  imageSrc: string,
  name: string,
  removed: boolean, // eslint-disable-next-line react/no-unused-prop-types
};

class Food extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  componentWillReceiveProps(nextProps: PropTypes) {
    if (nextProps.removed && this.node && !this.removed) {
      const node = this.node;
      // Programatically remove the eaten food instead of declaratively because we do not manage
      // the food's position in our state and cannot allow it to be rerendered.
      const drawingNode = node.getLayer() || node.getStage();
      node.remove();
      drawingNode.batchDraw();
      this.removed = true;
    }
  }

  // Draggable node position is managed by konva so we cannot rerender them.
  // Ideally, we could make the nodes draggable without Konva rerendering them, which would
  // allow the positions to be managed in our state, but this is not possible.
  shouldComponentUpdate() {
    return false;
  }

  props: PropTypes;

  node: ?Group = null;
  removed = false;

  render() {
    const { ingredient } = this.props;
    return (
      <Group
        key={ingredient}
        x={this.props.x}
        y={this.props.y}
        width={292}
        height={292}
        draggable
        onDragmove={(e) => this.props.onFoodDragged({
          ingredient,
          node: e.target,
        })}
        ref={(node) => { this.node = node; }}
      >
        <KonvaImage
          src={this.props.imageSrc}
          width={292}
          height={292}
        />
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
}

Food.propTypes = {

};

export default Food;
