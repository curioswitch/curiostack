/**
 *
 * HomePage
 *
 */

import React from 'react';
import { Helmet } from 'react-helmet';
import { connect } from 'react-redux';
import { Stage } from 'react-konva';
import { compose } from 'redux';

import injectSaga from 'utils/injectSaga';
import injectReducer from 'utils/injectReducer';

import FlowerLayer from 'components/FlowerLayer';
import FoodLayer from 'components/FoodLayer';
import MainLayer from 'components/MainLayer';

import { INGREDIENTS } from './constants';
import makeSelectHomePage from './selectors';
import reducer from './reducer';
import saga from './saga';

export class HomePage extends React.PureComponent { // eslint-disable-line react/prefer-stateless-function
  render() {
    return (
      <div>
        <Helmet>
          <title>HomePage</title>
          <meta name="description" content="Description of HomePage" />
        </Helmet>
        <Stage width={1080} height={1920}>
          <MainLayer />
          <FlowerLayer />
          <FoodLayer ingredients={INGREDIENTS.fruit} />
          <FoodLayer ingredients={INGREDIENTS.meat} visible={false} />
          <FoodLayer ingredients={INGREDIENTS.other} visible={false} />
        </Stage>
      </div>
    );
  }
}

const mapStateToProps = makeSelectHomePage();

function mapDispatchToProps(dispatch) {
  return {
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
