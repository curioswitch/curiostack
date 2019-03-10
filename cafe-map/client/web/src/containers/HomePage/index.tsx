/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
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
 *
 */

import { injectReducer, injectSaga } from '@curiostack/base-web';
import Grid from '@material-ui/core/Grid';
import React, { useCallback, useEffect } from 'react';
import Helmet from 'react-helmet';
import { hot } from 'react-hot-loader/root';
import { InjectedIntlProps, injectIntl } from 'react-intl';
import { connect } from 'react-redux';
import { compose } from 'redux';
import { InjectedFormProps } from 'redux-form';
import { Field, reduxForm } from 'redux-form/immutable';
import styled from 'styled-components';

import Map from '../../components/Map';
import SearchBox, { SearchBoxProps } from '../../components/SearchBox';

import { DispatchProps, mapDispatchToProps } from './actions';
import messages from './messages';
import reducer, { StateProps } from './reducer';
import saga from './saga';
import selectHomePage from './selectors';

type Props = DispatchProps & InjectedIntlProps & StateProps;

const SearchBoxWrapper = styled.div`
  position: absolute;
  top: 10px;
  width: 100%;
  height: 100%;
  z-index: 100;
  background: transparent;
  pointer-events: none;

  ${SearchBox} {
    pointer-events: auto;
  }
`;

const SearchBoxContainer: React.FunctionComponent<SearchBoxProps> = React.memo(
  (props) => (
    <SearchBoxWrapper>
      <Grid container justify="center" alignItems="center">
        <Grid item xs={11} md={6}>
          <Field name="query" component={SearchBox} {...props} />
        </Grid>
      </Grid>
    </SearchBoxWrapper>
  ),
);

const HomePage: React.FunctionComponent<Props & InjectedFormProps> = React.memo(
  (props) => {
    const {
      doSearch,
      getLandmarks,
      getPlaces,
      handleSubmit,
      landmarks,
      places,
      intl: { formatMessage: _ },
      setMap,
    } = props;

    useEffect(() => {
      getPlaces();
      return;
    }, []);

    const handleSearchSubmit = useCallback(handleSubmit(doSearch), [doSearch]);

    return (
      <>
        <Helmet title={_(messages.title)} />
        <form onSubmit={handleSearchSubmit}>
          <SearchBoxContainer onSearch={handleSearchSubmit} />
        </form>
        <Map
          doGetLandmarks={getLandmarks}
          doSetMap={setMap}
          landmarks={landmarks}
          places={places}
        />
      </>
    );
  },
);

const withConnect = connect(
  selectHomePage,
  mapDispatchToProps,
);
const withReducer = injectReducer({
  reducer,
  key: 'homePage',
});
const withSaga = injectSaga({ saga, key: 'homePage' });

export default compose(
  injectIntl,
  withReducer,
  withSaga,
  withConnect,
  reduxForm({
    form: 'homePage',
  }),
  hot,
)(HomePage);
