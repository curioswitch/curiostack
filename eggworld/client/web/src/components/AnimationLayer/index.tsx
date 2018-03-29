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
import { Layer } from 'react-konva';

import KonvaImage from '../KonvaImage';
import KonvaSprite from '../KonvaSprite';

import hammerImageSrc from './assets/hammer.png';
import mouthSpriteSrc from './assets/mouth-sprite.png';

interface Props {
  hammerRotation: number;
  onHammerClick: () => void;
  onMouthAnimationFrame: () => void;
  showHammer: boolean;
  started: boolean;
  visible: boolean;
}

export default class AnimationLayer extends React.PureComponent<Props> {
  public render() {
    return (
      <Layer visible={this.props.visible}>
        <KonvaSprite
          src={mouthSpriteSrc}
          x={450}
          y={390}
          animation={this.props.started ? 'eat' : 'idle'}
          animations={{
            idle: [0, 0, 170, 170],
            eat: [
              0,
              0,
              170,
              170,
              0,
              220,
              170,
              170,
              0,
              440,
              170,
              170,
              0,
              660,
              170,
              170,
              0,
              440,
              170,
              170,
              0,
              220,
              170,
              170,
            ],
          }}
          frameRate={10}
          onFrameIndexChange={this.props.onMouthAnimationFrame}
          started={this.props.started && this.props.visible}
        />
        <KonvaImage
          src={hammerImageSrc}
          x={300}
          y={250}
          width={300}
          height={200}
          offsetY={200}
          rotation={this.props.hammerRotation}
          visible={this.props.showHammer}
          onClick={this.props.onHammerClick}
        />
      </Layer>
    );
  }
}
