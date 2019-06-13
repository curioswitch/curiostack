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

import React, { useCallback } from 'react';
import { Layer } from 'react-konva';

import Button from '../Button';
import KonvaImage from '../KonvaImage';
import KonvaSprite from '../KonvaSprite';

import eggImageSrc from './assets/egg.png';
import eggBreakingSpriteSrc from './assets/eggbreaking-sprite.jpg';

interface Props {
  cooking: boolean;
  onEggBreakingDone: () => void;
  onSelectTab: (tab: 'fruit' | 'meat' | 'other') => void;
  selected: 'fruit' | 'meat' | 'other';
}

const MainLayer: React.FunctionComponent<Props> = (props) => {
  const { cooking, onEggBreakingDone, onSelectTab, selected } = props;

  const onSelectFruit = useCallback(() => {
    onSelectTab('fruit');
  }, [onSelectTab]);

  const onSelectMeat = useCallback(() => {
    onSelectTab('meat');
  }, [onSelectTab]);

  const onSelectOther = useCallback(() => {
    onSelectTab('other');
  }, [onSelectTab]);

  const onEggBreakingFrameChange = useCallback(
    (e: any) => {
      if (e.newVal === 3) {
        e.currentTarget.stop();
        onEggBreakingDone();
      }
    },
    [onEggBreakingDone],
  );

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
            0,
            0,
            1080,
            769,
            0,
            881,
            1080,
            769,
            0,
            1762,
            1080,
            769,
            0,
            2643,
            1080,
            769,
          ],
        }}
        frameRate={3}
        onFrameIndexChange={onEggBreakingFrameChange}
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
        onClick={onSelectFruit}
      />
      <Button
        selected={selected === 'meat'}
        x={360}
        y={720}
        label="肉・乳製品"
        onClick={onSelectMeat}
      />
      <Button
        selected={selected === 'other'}
        x={720}
        y={720}
        label="その他"
        onClick={onSelectOther}
      />
    </Layer>
  );
};

export default React.memo(MainLayer);
