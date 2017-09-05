/**
 *
 * HomePage
 *
 */

// @flow

import type { Node } from 'konva';
import type { Dispatch } from 'redux';

import type { Ingredient } from 'curioswitch-eggworld-api/curioswitch/eggworld/eggworld-service_pb';

import { Animation } from 'konva';
import React from 'react';
import { Helmet } from 'react-helmet';
import { connect } from 'react-redux';
import { Stage } from 'react-konva';
import { compose } from 'redux';

import injectSaga from 'utils/injectSaga';
import injectReducer from 'utils/injectReducer';

import AnimationLayer from 'components/AnimationLayer';
import FlowerLayer from 'components/FlowerLayer';
import FoodLayer from 'components/FoodLayer';
import MainLayer from 'components/MainLayer';

import {
  checkIngredients,
  foodDragged,
  mouthAnimationFrame,
  rotateHammer,
  selectTab,
} from './actions';
import { INGREDIENTS } from './constants';
import makeSelectHomePage from './selectors';
import reducer from './reducer';
import saga from './saga';

type Props = {
  doCheckIngredients: (number[]) => void,
  doRotateHammer: (number) => void,
  eatenFood: Ingredient[],
  foodBeingEaten: ?Node,
  hammerRotation: number,
  onFoodDragged: (Node) => void,
  onMouthAnimationFrame: () => void,
  onSelectTab: (string) => void,
  selectedTab: 'fruit' | 'meat' | 'other',
  usableFood: Ingredient[],
};

export class HomePage extends React.PureComponent<Props> { // eslint-disable-line react/prefer-stateless-function
  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.eatenFood.length !== this.props.eatenFood.length) {
      this.props.doCheckIngredients(nextProps.eatenFood);
    }
    if (nextProps.eatenFood.length > 0 && !this.hammerAnimation.isRunning()) {
      this.hammerAnimation.start();
    }
  }

  hammerAnimation = new Animation((frame) => {
    let angleDiff = frame.timeDiff * 360 / 10 / 1000;
    const frameIndex = frame.time % 3000;
    if (frameIndex < 200) {
    } else if (frameIndex < 400) {
      angleDiff = -angleDiff;
    } else if (frameIndex < 600) {
      angleDiff = 0;
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
    return (
      <div>
        <Helmet>
          <title>HomePage</title>
          <meta name="description" content="Description of HomePage" />
        </Helmet>
        <Stage width={1080} height={1920}>
          <MainLayer selected={this.props.selectedTab} onSelectTab={this.props.onSelectTab} />
          <FlowerLayer eatenFood={this.props.eatenFood} />
          <AnimationLayer
            onMouthAnimationFrame={this.props.onMouthAnimationFrame}
            hammerRotation={this.props.hammerRotation}
            showHammer={this.props.eatenFood.length !== 0}
            started={this.props.foodBeingEaten !== null}
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
    onFoodDragged: (food: Node) => dispatch(foodDragged(food)),
    doRotateHammer: (angleDiff: number) => dispatch(rotateHammer(angleDiff)),
    onMouthAnimationFrame: () => dispatch(mouthAnimationFrame()),
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
