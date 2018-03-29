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

import { ShapeConfig, Sprite as SpriteImpl } from 'konva';
import React from 'react';
import { Sprite } from 'react-konva';

interface Props extends ShapeConfig {
  animation: string;
  animations: any;
  frameRate: number;
  onFrameIndexChange: (e: any) => void;
  src: string;
  started: boolean;
}

interface State {
  image?: HTMLImageElement;
}

class KonvaSprite extends React.PureComponent<Props, State> {
  public state: State = {
    image: undefined,
  };

  private node?: SpriteImpl = undefined;

  public componentDidMount() {
    const image = new Image();
    image.onload = () => {
      this.setState({
        image,
      });
    };
    image.src = this.props.src;
    // While react-konva is supposed to handle this, it doesn't for some reason.
    if (this.node) {
      this.node.on('frameIndexChange', this.props.onFrameIndexChange);
    }
  }

  public componentWillReceiveProps(nextProps: Props) {
    if (nextProps.started === this.props.started || !this.node) {
      return;
    }
    if (nextProps.started) {
      this.node.start();
    } else {
      this.node.stop();
    }
  }

  public render() {
    const { onFrameIndexChange, src, ...others } = this.props;
    return (
      <>
        {this.state.image ? (
          <Sprite
            // TODO(choko): See if this any can be avoided.
            ref={(node: any) => {
              this.node = node || undefined;
            }}
            image={this.state.image}
            {...others}
          />
        ) : null}
      </>
    );
  }
}

export default KonvaSprite;
