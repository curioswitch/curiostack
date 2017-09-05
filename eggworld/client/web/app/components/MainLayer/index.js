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

import eggImageSrc from './assets/egg.png';

type Props = {
  onSelectTab: (string) => void,
  selected: 'fruit' | 'meat' | 'other',
};

class MainLayer extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  render() {
    const { selected } = this.props;
    return (
      <Layer>
        <KonvaImage
          src={eggImageSrc}
          width={1080}
          height={760}
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
