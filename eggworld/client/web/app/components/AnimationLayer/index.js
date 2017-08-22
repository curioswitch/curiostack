/**
*
* AnimationLayer
*
*/

// @flow

import React from 'react';
import { Layer } from 'react-konva';

import KonvaSprite from 'components/KonvaSprite';

import mouthSpriteSrc from './assets/mouth-sprite.png';

class AnimationLayer extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  props: {
    onMouthAnimationFrame: () => void,
    started: boolean,
  };

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
      </Layer>
    );
  }
}

export default AnimationLayer;
