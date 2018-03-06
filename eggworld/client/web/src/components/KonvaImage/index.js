/**
*
* KonvaImage
*
*/

// @flow

import React from 'react';
import { Image } from 'react-konva';

type Props = {
  src: string,
};

type State = {
  image: ?window.Image,
};

class KonvaImage extends React.PureComponent<Props, State> { // eslint-disable-line react/prefer-stateless-function
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

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.src === this.props.src || !this.state.image) {
      return;
    }
    this.state.image.src = nextProps.src;
  }

  render() {
    return (
      <Image image={this.state.image} {...this.props} />
    );
  }
}

export default KonvaImage;
