/**
 *
 * AnimationLayer
 *
 */

// @flow

import React from 'react';
import {Layer} from 'react-konva';

import KonvaImage from 'components/KonvaImage';
import KonvaSprite from 'components/KonvaSprite';

import hammerImageSrc from './assets/hammer.png';
import mouthSpriteSrc from './assets/mouth-sprite.png';

type Props = {
  hammerRotation: number,
  onMouthAnimationFrame: () => void,
  showHammer: boolean,
  started: boolean,
};

class AnimationLayer extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <Layer>
        <KonvaSprite
          src={mouthSpriteSrc}
          x={450}
          y={390}
          animation={this.props.started ? 'eat' : 'idle'}
          animations={{
            idle: [
              0, 0, 170, 170,
            ],
            eat: [
              0, 0, 170, 170,
              0, 220, 170, 170,
              0, 440, 170, 170,
              0, 660, 170, 170,
              0, 440, 170, 170,
              0, 220, 170, 170,
            ],
          }}
          frameRate={10}
          onFrameIndexChange={this.props.onMouthAnimationFrame}
          started={this.props.started}
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
        />
      </Layer>
    );
  }
}

export default AnimationLayer;
