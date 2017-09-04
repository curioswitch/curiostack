/**
*
* KonvaSprite
*
*/

import React from 'react';
import { Sprite } from 'react-konva';

type PropTypes = {
  animation?: string,
  onFrameIndexChange: (any) => void,
  src: string,
  started: boolean,
};

class KonvaSprite extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function
  state = {
    image: null,
  };

  componentDidMount() {
    const image = new window.Image();
    image.onload = () => {
      this.setState({
        image,
      });
    };
    image.src = this.props.src;
    // While react-konva is supposed to handle this, it doesn't for some reason.
    this.node.on('frameIndexChange', this.props.onFrameIndexChange);
  }

  componentWillReceiveProps(nextProps: PropTypes) {
    if (nextProps.started === this.props.started) {
      return;
    }
    if (nextProps.started) {
      this.node.start();
    } else {
      this.node.stop();
    }
  }

  node: ?Sprite = null;

  props: PropTypes;

  render() {
    // eslint-disable-next-line no-unused-vars
    const { onFrameIndexChange, src, ...others } = this.props;
    return (
      <Sprite ref={(node) => { this.node = node; }} image={this.state.image} {...others} />
    );
  }
}

export default KonvaSprite;
