/**
*
* KonvaImage
*
*/

// @flow

import React from 'react';
import { Image } from 'react-konva';

type PropTypes = {
  src: string,
};

class KonvaImage extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  state = {
    image: null,
  };

  componentDidMount() {
    const image = new window.Image();
    image.onload = () => {
      if (!this.state.image) {
        this.setState({
          image,
        });
      }
    };
    image.src = this.props.src;
  }

  componentWillReceiveProps(nextProps: PropTypes) {
    if (nextProps.src === this.props.src || !this.state.image) {
      return;
    }
    this.state.image.src = nextProps.src;
  }

  props: PropTypes;

  render() {
    return (
      <Image image={this.state.image} {...this.props} />
    );
  }
}

export default KonvaImage;
