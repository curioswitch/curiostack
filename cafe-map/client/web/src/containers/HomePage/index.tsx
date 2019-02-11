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

import React from 'react';
import Helmet from 'react-helmet';
import { hot } from 'react-hot-loader/root';
import { FormattedMessage, InjectedIntlProps, injectIntl } from 'react-intl';
import { connect } from 'react-redux';
import { compose } from 'redux';

import Map from '../../components/Map';

import { DispatchProps, mapDispatchToProps } from './actions';
import messages from './messages';
import reducer from './reducer';
import saga from './saga';
import selectHomePage from './selectors';

type Props = DispatchProps & InjectedIntlProps;

const HomePage: React.FunctionComponent<Props> = React.memo((props) => {
  const {
    intl: { formatMessage: _ },
  } = props;
  return (
    <>
      <Helmet title={_(messages.title)} />
      <h1>
        <FormattedMessage {...messages.header} />
      </h1>
      <Map />
    </>
  );
});

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
  hot,
)(HomePage);
