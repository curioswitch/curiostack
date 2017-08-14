/**
*
* KonvaImage
*
*/

// @flow

import React from 'react';
import { Image } from 'react-konva';

class KonvaImage extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

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
  }

  props: {
    src: string,
  };

  render() {
    // eslint-disable-next-line no-unused-vars
    const { src, ...others } = this.props;
    return (
      <Image image={this.state.image} {...others} />
    );
  }
}

export default KonvaImage;
