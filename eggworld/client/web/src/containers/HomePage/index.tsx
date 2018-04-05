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
import { Animation } from 'konva';
import React from 'react';
import { Helmet } from 'react-helmet';
import { hot } from 'react-hot-loader';
import { Stage } from 'react-konva';
import { connect } from 'react-redux';
import { compose } from 'redux';

import { injectReducer, injectSaga } from '@curiostack/base-web';

import AnimationLayer from '../../components/AnimationLayer';
import FlowerLayer from '../../components/FlowerLayer';
import FoodLayer from '../../components/FoodLayer';
import MainLayer from '../../components/MainLayer';

import { DispatchProps, mapDispatchToProps } from './actions';
import { INGREDIENTS } from './constants';
import reducer, { State } from './reducer';
import saga from './saga';
import selectHomePage from './selectors';

import mogmogChokoSoundSrc from './assets/mogmog_choko.m4a';
import mogmogCute1SoundSrc from './assets/mogmog_cute1.m4a';
import mogmogNormal1SoundSrc from './assets/mogmog_normal1.m4a';

// tslint:disable-next-line:no-var-requires
const baconTest = require('./assets/bacon.png?xs=100&sm=33').default;
// tslint:disable-next-line:no-var-requires
const baconTest2 = require('./assets/bacon.png?xs=100&lossy=true').default;
console.log(baconTest);
console.log(baconTest2);

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

type Props = State & DispatchProps;

export class HomePage extends React.PureComponent<Props> {
  public hammerAnimation = new Animation((frame: any) => {
    let angleDiff = frame.timeDiff * 360 / 10 / 1000;
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
      this.props.rotateHammer(angleDiff);
    }
  });

  public componentWillReceiveProps(nextProps: Props) {
    if (nextProps.eatenFood.size !== this.props.eatenFood.size) {
      this.props.checkIngredients(nextProps.eatenFood.toArray());
    }
    if (!nextProps.eatenFood.isEmpty() && !this.hammerAnimation.isRunning()) {
      this.hammerAnimation.start();
    }
    if (!this.props.cooking && nextProps.cooking) {
      this.hammerAnimation.stop();
    }
    if (!this.props.foodBeingEaten && nextProps.foodBeingEaten) {
      SOUNDS[getRandomInt(0, 2)].play();
    }
  }

  public render() {
    const { cooking } = this.props;
    return (
      <div>
        <Helmet>
          <title>HomePage</title>
          <meta name="description" content="Description of HomePage" />
        </Helmet>
        <Stage width={1080} height={1920}>
          <MainLayer
            selected={this.props.selectedTab}
            cooking={cooking}
            onEggBreakingDone={this.props.eggBreakingDone}
            onSelectTab={this.props.selectTab}
          />
          <FlowerLayer eatenFood={this.props.eatenFood} visible={!cooking} />
          <AnimationLayer
            onHammerClick={this.props.cook}
            onMouthAnimationFrame={this.props.mouthAnimationFrame}
            hammerRotation={this.props.hammerRotation}
            showHammer={!this.props.eatenFood.isEmpty()}
            started={!!this.props.foodBeingEaten}
            visible={!cooking}
          />
          <FoodLayer
            ingredients={INGREDIENTS.fruit}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.foodDragged}
            visible={this.props.selectedTab === 'fruit'}
          />
          <FoodLayer
            ingredients={INGREDIENTS.meat}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.foodDragged}
            visible={this.props.selectedTab === 'meat'}
          />
          <FoodLayer
            ingredients={INGREDIENTS.other}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.foodDragged}
            visible={this.props.selectedTab === 'other'}
          />
        </Stage>
      </div>
    );
  }
}

const withConnect = connect(selectHomePage, mapDispatchToProps);

const withReducer = injectReducer({ reducer, key: 'homePage' });
const withSaga = injectSaga({ saga, key: 'homePage' });

export default compose(hot(module), withReducer, withSaga, withConnect)(
  HomePage,
);
