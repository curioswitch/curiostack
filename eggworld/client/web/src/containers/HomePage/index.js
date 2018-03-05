/**
 *
 * HomePage
 *
 */

// @flow

import type { Node } from 'konva';
import type { Dispatch } from 'redux';

import type { Ingredient } from '@curiostack/eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import 'yuki-createjs/lib/soundjs-0.6.2.combined';

import { Animation } from 'konva';
import React from 'react';
import { Helmet } from 'react-helmet';
import { connect } from 'react-redux';
import { Stage } from 'react-konva';
import { compose } from 'redux';

import injectSaga from '@curiostack/base-web/hoc/injectSaga';
import injectReducer from '@curiostack/base-web/hoc/injectReducer';

import AnimationLayer from 'components/AnimationLayer';
import FlowerLayer from 'components/FlowerLayer';
import FoodLayer from 'components/FoodLayer';
import MainLayer from 'components/MainLayer';

import {
  checkIngredients,
  cook,
  eggBreakingDone,
  foodDragged,
  mouthAnimationFrame,
  rotateHammer,
  selectTab,
} from './actions';
import { INGREDIENTS } from './constants';
import makeSelectHomePage from './selectors';
import reducer from './reducer';
import saga from './saga';

import mogmogChokoSoundSrc from './assets/mogmog_choko.m4a';
import mogmogCute1SoundSrc from './assets/mogmog_cute1.m4a';
import mogmogNormal1SoundSrc from './assets/mogmog_normal1.m4a';

type Props = {
  cooking: boolean,
  doCheckIngredients: (number[]) => void,
  doCook: () => void,
  doRotateHammer: (number) => void,
  eatenFood: Ingredient[],
  foodBeingEaten: ?Node,
  hammerRotation: number,
  onEggBreakingDone: () => void,
  onFoodDragged: (Node) => void,
  onMouthAnimationFrame: () => void,
  onSelectTab: (string) => void,
  selectedTab: 'fruit' | 'meat' | 'other',
  usableFood: Ingredient[],
};

const createjs = global.createjs;

createjs.Sound.registerSound(mogmogChokoSoundSrc, 'mogmog1');
createjs.Sound.registerSound(mogmogCute1SoundSrc, 'mogmog2');
createjs.Sound.registerSound(mogmogNormal1SoundSrc, 'mogmog3');

function getRandomInt(min, max) {
  const minCeil = Math.ceil(min);
  const maxFloor = Math.floor(max);
  return Math.floor(Math.random() * ((maxFloor - minCeil) + 1)) + minCeil;
}

export class HomePage extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.eatenFood.length !== this.props.eatenFood.length) {
      this.props.doCheckIngredients(nextProps.eatenFood);
    }
    if (nextProps.eatenFood.length > 0 && !this.hammerAnimation.isRunning()) {
      this.hammerAnimation.start();
    }
    if (!this.props.cooking && nextProps.cooking) {
      this.hammerAnimation.stop();
    }
    if (!this.props.foodBeingEaten && nextProps.foodBeingEaten) {
      createjs.Sound.play(`mogmog${getRandomInt(1, 3)}`);
    }
  }

  hammerAnimation = new Animation((frame) => {
    let angleDiff = ((frame.timeDiff * 360) / 10) / 1000;
    const frameIndex = frame.time % 3000;
    // eslint-disable-next-line no-empty
    if (frameIndex < 200) {
    } else if (frameIndex < 400) {
      angleDiff = -angleDiff;
    } else if (frameIndex < 600) {
      angleDiff = 0;
    // eslint-disable-next-line no-empty
    } else if (frameIndex < 800) {
    } else if (frameIndex < 1000) {
      angleDiff = -angleDiff;
    } else {
      angleDiff = 0;
    }
    if (angleDiff !== 0) {
      this.props.doRotateHammer(angleDiff);
    }
  });

  render() {
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
            onEggBreakingDone={this.props.onEggBreakingDone}
            onSelectTab={this.props.onSelectTab}
          />
          <FlowerLayer
            eatenFood={this.props.eatenFood}
            visible={!cooking}
          />
          <AnimationLayer
            onHammerClick={this.props.doCook}
            onMouthAnimationFrame={this.props.onMouthAnimationFrame}
            hammerRotation={this.props.hammerRotation}
            showHammer={this.props.eatenFood.length !== 0}
            started={this.props.foodBeingEaten !== null}
            visible={!cooking}
          />
          <FoodLayer
            ingredients={INGREDIENTS.fruit}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.onFoodDragged}
            visible={this.props.selectedTab === 'fruit'}
          />
          <FoodLayer
            ingredients={INGREDIENTS.meat}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.onFoodDragged}
            visible={this.props.selectedTab === 'meat'}
          />
          <FoodLayer
            ingredients={INGREDIENTS.other}
            eatenFood={this.props.eatenFood}
            usableFood={this.props.usableFood}
            onFoodDragged={this.props.onFoodDragged}
            visible={this.props.selectedTab === 'other'}
          />
        </Stage>
      </div>
    );
  }
}

const mapStateToProps = makeSelectHomePage();

function mapDispatchToProps(dispatch: Dispatch<*>) {
  return {
    doCheckIngredients: (ingredients: number[]) => dispatch(checkIngredients(ingredients)),
    doCook: () => dispatch(cook()),
    onFoodDragged: (food: Node) => dispatch(foodDragged(food)),
    doRotateHammer: (angleDiff: number) => dispatch(rotateHammer(angleDiff)),
    onMouthAnimationFrame: () => dispatch(mouthAnimationFrame()),
    onEggBreakingDone: () => dispatch(eggBreakingDone()),
    onSelectTab: (tab: string) => dispatch(selectTab(tab)),
    dispatch,
  };
}

const withConnect = connect(mapStateToProps, mapDispatchToProps);

const withReducer = injectReducer({ key: 'homePage', reducer });
const withSaga = injectSaga({ key: 'homePage', saga });

export default compose(
  withReducer,
  withSaga,
  withConnect,
)(HomePage);
