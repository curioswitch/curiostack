/**
*
* MainLayer
*
*/

// @flow

import React from 'react';
import { Layer } from 'react-konva';

import Button from 'components/Button';
import KonvaImage from 'components/KonvaImage';
import KonvaSprite from 'components/KonvaSprite';

import eggImageSrc from './assets/egg.png';
import eggBreakingSpriteSrc from './assets/eggbreaking-sprite.jpg';

type Props = {
  cooking: boolean,
  onEggBreakingDone: () => void,
  onSelectTab: (string) => void,
  selected: 'fruit' | 'meat' | 'other',
};

class MainLayer extends React.PureComponent<Props> {
  onEggBreakingFrameChange = (e: any) => {
    if (e.newVal === 3) {
      e.currentTarget.stop();
      this.props.onEggBreakingDone();
    }
  };

  render() {
    const { cooking, selected } = this.props;
    return (
      <Layer>
        <KonvaImage
          src={eggImageSrc}
          width={1080}
          height={760}
          visible={!cooking}
        />
        <KonvaSprite
          src={eggBreakingSpriteSrc}
          animation="break"
          animations={{
            break: [
              0, 0, 1080, 769,
              0, 881, 1080, 769,
              0, 1762, 1080, 769,
              0, 2643, 1080, 769,
            ],
          }}
          frameRate={3}
          onFrameIndexChange={this.onEggBreakingFrameChange}
          started={cooking}
          visible={cooking}
        />
        <KonvaImage
          src="http://static.yummly.com/api-logo.png"
          x={884}
          y={1880}
          width={196}
          height={40}
        />
        <Button
          selected={selected === 'fruit'}
          x={0}
          y={720}
          label="果物・野菜"
          onClick={() => this.props.onSelectTab('fruit')}
        />
        <Button
          selected={selected === 'meat'}
          x={360}
          y={720}
          label="肉・乳製品"
          onClick={() => this.props.onSelectTab('meat')}
        />
        <Button
          selected={selected === 'other'}
          x={720}
          y={720}
          label="その他"
          onClick={() => this.props.onSelectTab('other')}
        />
      </Layer>
    );
  }
}

export default MainLayer;
