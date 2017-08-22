/**
 *
 * HomePage
 *
 */

// @flow

import type { Set } from 'immutable';
import type { Node } from 'konva';
import type { Dispatch } from 'redux';

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
  foodDragged,
  mouthAnimationFrame,
} from './actions';
import { INGREDIENTS } from './constants';
import makeSelectHomePage from './selectors';
import reducer from './reducer';
import saga from './saga';

export class HomePage extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  props: {
    foodBeingEaten: ?Node,
    onFoodDragged: (Node) => void,
    onMouthAnimationFrame: () => void,
  };

  render() {
    return (
      <div>
        <Helmet>
          <title>HomePage</title>
          <meta name="description" content="Description of HomePage" />
        </Helmet>
        <Stage width={1080} height={1920}>
          <MainLayer selected="fruit" />
          <FlowerLayer />
          <AnimationLayer
            onMouthAnimationFrame={this.props.onMouthAnimationFrame}
            started={this.props.foodBeingEaten !== null}
          />
          <FoodLayer
            ingredients={INGREDIENTS.fruit}
            onFoodDragged={this.props.onFoodDragged}
          />
          <FoodLayer
            ingredients={INGREDIENTS.meat}
            onFoodDragged={this.props.onFoodDragged}
            visible={false}
          />
          <FoodLayer
            ingredients={INGREDIENTS.other}
            onFoodDragged={this.props.onFoodDragged}
            visible={false}
          />
        </Stage>
      </div>
    );
  }
}

const mapStateToProps = makeSelectHomePage();

function mapDispatchToProps(dispatch: Dispatch<*>) {
  return {
    onFoodDragged: (food: Node) => dispatch(foodDragged(food)),
    onMouthAnimationFrame: () => dispatch(mouthAnimationFrame()),
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
