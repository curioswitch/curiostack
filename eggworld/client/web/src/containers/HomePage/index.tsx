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

import { Howl } from 'howler';
import Konva from 'konva';
import React, { useEffect, useMemo } from 'react';
import { Helmet } from 'react-helmet';
import { hot } from 'react-hot-loader/root';
import { Stage } from 'react-konva';
import { useDispatch, useSelector } from 'react-redux';
import { bindActionCreators } from 'redux';

import { useReducer, useSaga } from '@curiostack/base-web';

import AnimationLayer from '../../components/AnimationLayer';
import FlowerLayer from '../../components/FlowerLayer';
import FoodLayer from '../../components/FoodLayer';
import MainLayer from '../../components/MainLayer';

import Actions from './actions';
import { INGREDIENTS } from './constants';
import reducer, { State } from './reducer';
import saga from './saga';
import selectHomePage from './selectors';

import mogmogChokoSoundSrc from './assets/mogmog_choko.m4a';
import mogmogCute1SoundSrc from './assets/mogmog_cute1.m4a';
import mogmogNormal1SoundSrc from './assets/mogmog_normal1.m4a';

const SOUNDS = [
  new Howl({
    src: [mogmogChokoSoundSrc],
  }),
  new Howl({
    src: [mogmogCute1SoundSrc],
  }),
  new Howl({
    src: [mogmogNormal1SoundSrc],
  }),
];

function getRandomInt(min: number, max: number) {
  const minCeil = Math.ceil(min);
  const maxFloor = Math.floor(max);
  return Math.floor(Math.random() * (maxFloor - minCeil + 1)) + minCeil;
}

type Props = State;

const HomePage: React.FunctionComponent<Props> = () => {
  const dispatch = useDispatch();

  useReducer({ reducer, key: 'homePage' });
  useSaga({ saga, key: 'homePage' });

  const dispatchActions = useMemo(() => bindActionCreators(Actions, dispatch), [
    dispatch,
  ]);

  const state = useSelector(selectHomePage);

  const { cooking, eatenFood, foodBeingEaten } = state;

  const hammerAnimation = useMemo(
    () =>
      new Konva.Animation((frame: any) => {
        let angleDiff = (frame.timeDiff * 360) / 10 / 1000;
        const frameIndex = frame.time % 3000;
        if (frameIndex < 200) {
          // No-op
        } else if (frameIndex < 400) {
          angleDiff = -angleDiff;
        } else if (frameIndex < 600) {
          angleDiff = 0;
        } else if (frameIndex < 800) {
          // No-op
        } else if (frameIndex < 1000) {
          angleDiff = -angleDiff;
        } else {
          angleDiff = 0;
        }
        if (angleDiff !== 0) {
          dispatchActions.rotateHammer(angleDiff);
        }
      }),
    [dispatch],
  );

  useEffect(() => {
    dispatchActions.checkIngredients(eatenFood.toArray());

    if (!hammerAnimation.isRunning()) {
      hammerAnimation.start();
    }
  }, [eatenFood]);

  useEffect(() => {
    if (hammerAnimation.isRunning() && cooking) {
      hammerAnimation.stop();
    }
  }, [cooking]);

  useEffect(() => {
    if (foodBeingEaten !== undefined) {
      SOUNDS[getRandomInt(0, 2)].play();
    }
  }, [foodBeingEaten]);

  let width = 1080;
  let height = 1920;
  let scale = 1.0;

  const app = document.getElementById('app');
  if (app) {
    const scaleWidth = app.offsetWidth / 1080;
    const scaleHeight = app.offsetHeight / 1920;

    scale = Math.min(scaleWidth, scaleHeight);
    width = scale * 1080;
    height = scale * 1920;
  }

  return (
    <>
      <Helmet>
        <title>HomePage</title>
        <meta name="description" content="Description of HomePage" />
      </Helmet>
      <Stage width={width} height={height} scaleX={scale} scaleY={scale}>
        <MainLayer
          selected={state.selectedTab}
          cooking={cooking}
          onEggBreakingDone={dispatchActions.eggBreakingDone}
          onSelectTab={dispatchActions.selectTab}
        />
        <FlowerLayer eatenFood={state.eatenFood} visible={!cooking} />
        <AnimationLayer
          onHammerClick={dispatchActions.cook}
          onMouthAnimationFrame={dispatchActions.mouthAnimationFrame}
          hammerRotation={state.hammerRotation}
          showHammer={!state.eatenFood.isEmpty()}
          started={state.foodBeingEaten !== undefined}
          visible={!cooking}
        />
        <FoodLayer
          ingredients={INGREDIENTS.fruit}
          eatenFood={state.eatenFood}
          usableFood={state.usableFood}
          onFoodDragged={dispatchActions.foodDragged}
          visible={state.selectedTab === 'fruit'}
        />
        <FoodLayer
          ingredients={INGREDIENTS.meat}
          eatenFood={state.eatenFood}
          usableFood={state.usableFood}
          onFoodDragged={dispatchActions.foodDragged}
          visible={state.selectedTab === 'meat'}
        />
        <FoodLayer
          ingredients={INGREDIENTS.other}
          eatenFood={state.eatenFood}
          usableFood={state.usableFood}
          onFoodDragged={dispatchActions.foodDragged}
          visible={state.selectedTab === 'other'}
        />
      </Stage>
    </>
  );
};

export default hot(HomePage);
