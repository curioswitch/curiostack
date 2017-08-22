/**
 *
 * HomePage
 *
 */

// @flow

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
  checkIngredients,
  foodDragged,
  mouthAnimationFrame,
  selectTab,
} from './actions';
import { INGREDIENTS } from './constants';
import makeSelectHomePage from './selectors';
import reducer from './reducer';
import saga from './saga';

type PropTypes = {
  doCheckIngredients: (number[]) => void,
  eatenFood: number[],
  foodBeingEaten: ?Node,
  onFoodDragged: (Node) => void,
  onMouthAnimationFrame: () => void,
  onSelectTab: (string) => void,
  selectedTab: 'fruit'|'meat'|'other',
  usableFood: number[],
};

export class HomePage extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function

  componentWillReceiveProps(nextProps: PropTypes) {
    if (nextProps.eatenFood.length !== this.props.eatenFood.length) {
      this.props.doCheckIngredients(nextProps.eatenFood);
    }
  }

  props: PropTypes;

  render() {
    return (
      <div>
        <Helmet>
          <title>HomePage</title>
          <meta name="description" content="Description of HomePage" />
        </Helmet>
        <Stage width={1080} height={1920}>
          <MainLayer selected={this.props.selectedTab} onSelectTab={this.props.onSelectTab} />
          <FlowerLayer />
          <AnimationLayer
            onMouthAnimationFrame={this.props.onMouthAnimationFrame}
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
