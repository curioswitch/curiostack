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

import { NodeConfig, ShapeConfig } from 'konva';
import React from 'react';
import { Image as ReactKonvaImage, KonvaNodeEvents } from 'react-konva';

interface Props extends ShapeConfig, NodeConfig, KonvaNodeEvents {
  src: string;
}

interface State {
  image?: HTMLImageElement;
}

class KonvaImage extends React.PureComponent<Props, State> {
  public state: State = {
    image: undefined,
  };

  public componentDidMount() {
    const image = new Image();
    image.onload = () => {
      if (!this.state.image) {
        this.setState({
          image,
        });
      }
    };
    image.src = this.props.src;
  }

  public componentWillReceiveProps(nextProps: Props) {
    if (nextProps.src === this.props.src || !this.state.image) {
      return;
    }
    this.state.image.src = nextProps.src;
  }

  public render() {
    return (
      <>
        {this.state.image ? (
          <ReactKonvaImage image={this.state.image} {...this.props} />
        ) : null}
      </>
    );
  }
}

export default KonvaImage;
